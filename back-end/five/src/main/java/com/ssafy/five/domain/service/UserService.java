package com.ssafy.five.domain.service;

import com.ssafy.five.controller.dto.req.*;
import com.ssafy.five.controller.dto.res.FindUserResDto;
import com.ssafy.five.domain.entity.EnumType.EvalType;
import com.ssafy.five.domain.entity.EnumType.StateType;
import com.ssafy.five.domain.entity.Messages;
import com.ssafy.five.domain.entity.ProfileImg;
import com.ssafy.five.domain.entity.Users;
import com.ssafy.five.domain.repository.SmsRepository;
import com.ssafy.five.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ssafy.five.util.SecurityUtil.getCurrentUserId;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final MailService mailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsRepository smsRepository;

    @Transactional
    public ResponseEntity<?> signUp(SignUpReqDto signUpReqDto) {
        if (userRepository.existsById(signUpReqDto.getUserId())) {
            log.info("이미 존재하는 아이디입니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }

        if(userRepository.existsByNickname(signUpReqDto.getNickname())){
            log.info("이미 존재하는 닉네임입니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmailIdAndEmailDomain(signUpReqDto.getEmailId(), signUpReqDto.getEmailDomain())) {
            log.info("중복된 이메일입니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByNumber(signUpReqDto.getNumber()) != null){
            log.info("중복된 전화번호입니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }

        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1);

        Users user = Users.builder()
                .userId(signUpReqDto.getUserId())
                .password(passwordEncoder.encode(signUpReqDto.getPassword()))
                .birth(signUpReqDto.getBirth())
                .emailId(signUpReqDto.getEmailId())
                .emailDomain(signUpReqDto.getEmailDomain())
                .endDate(new Date(cal.getTimeInMillis()))
                .nickname(signUpReqDto.getNickname())
                .ment(signUpReqDto.getMent())
                .number(signUpReqDto.getNumber())
                .genderType(signUpReqDto.getGenderType())
                .stateType(StateType.NORMAL)
                .roles(Collections.singletonList("ROLE_USER"))
                .profileImg(ProfileImg.builder()
                        .fileName("defaultProfileImg.png")
                        .build())
                .build();


        Messages msg = smsRepository.findById(user.getNumber()).orElseThrow(() -> new RuntimeException("인증되지 않은 휴대폰"));

        if (!msg.isAuth()) {
            return new ResponseEntity<>(false, HttpStatus.UNAUTHORIZED);
        }
        userRepository.save(user);
        smsRepository.delete(msg);
        log.info("정상 회원가입되었습니다.");
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<?> availableUserId(String userId) {

        Users user = userRepository.findByUserId(userId);

        if (user != null) {
            log.info("이미 존재하는 아이디입니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }

        log.info("사용 가능한 아이디입니다.");
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<?> findUser(String userId) {
        Users user = userRepository.findByUserId(userId);

        if (user != null) {
            FindUserResDto findUserResDto = FindUserResDto.builder()
                    .nickname(user.getNickname())
                    .ment(user.getMent())
                    .genderType(user.getGenderType())
                    .point(user.getPoint())
                    .stateType(user.getStateType())
                    .reportCount(user.getReportCount())
                    .endDate(user.getEndDate())
                    .profileImg(user.getProfileImg())
                    .roles(user.getRoles())
                    .build();
            return new ResponseEntity<>(findUserResDto, HttpStatus.OK);
        }
        log.info("존재하지 않는 유저입니다.");
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    @Transactional
    public ResponseEntity<?> updateUser(UpdateUserReqDto updateUserReqDto) {
        Users user1 = userRepository.findByUserId(updateUserReqDto.getUserId());
        if (user1 != null) {
            if(user1.getUserId().equals(getCurrentUserId())){
                if(!user1.getNickname().equals(updateUserReqDto.getNickname())){
                    if (userRepository.existsByNickname(updateUserReqDto.getNickname())){
                        log.info("중복된 닉네임입니다.");
                        return new ResponseEntity<>(false, HttpStatus.CONFLICT);
                    }
                }

                if(!(user1.getEmailId().equals(updateUserReqDto.getEmailId()) && user1.getEmailDomain().equals(updateUserReqDto.getEmailDomain()))){
                    if (userRepository.existsByEmailIdAndEmailDomain(updateUserReqDto.getEmailId(), updateUserReqDto.getEmailDomain())){
                        log.info("중복된 이메일입니다.");
                        return new ResponseEntity<>(false, HttpStatus.CONFLICT);
                    }
                }

                if (!user1.getNumber().equals(updateUserReqDto.getNumber())) {
                    if(userRepository.existsByNumber(updateUserReqDto.getNumber())) {
                        log.info("이미 사용중인 전화번호입니다.");
                        return new ResponseEntity<>(false, HttpStatus.CONFLICT);
                    }
                    Messages msg = smsRepository.findByReceiver(updateUserReqDto.getNumber());
                    if(msg == null) {
                        log.info("인증받지 않은 전화번호입니다.");
                        return new ResponseEntity<>(false, HttpStatus.UNAUTHORIZED);
                    }
                    if (!msg.isAuth()) {
                        log.info("인증번호를 다시 입력해주세요.");
                        return new ResponseEntity<>(false, HttpStatus.UNAUTHORIZED);
                    }
                    user1.updateNumber(updateUserReqDto.getNumber());
                    smsRepository.delete(msg);
                }

                user1.updatePassword(passwordEncoder.encode(updateUserReqDto.getPassword()));
                user1.updateEmailId(updateUserReqDto.getEmailId());
                user1.updateEmailDomain(updateUserReqDto.getEmailDomain());
                user1.updateNickname(updateUserReqDto.getNickname());
                user1.updateMent(updateUserReqDto.getMent());

                log.info("회원 정보가 수정되었습니다.");
                return new ResponseEntity<>(true, HttpStatus.OK);
            }
            log.info("본인을 제외한 다른 유저의 정보를 수정할 수 없습니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        log.info("존재하지 않는 유저입니다.");
        return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
    }

    @Transactional
    public ResponseEntity<?> deleteUser(String userId) {
        Users user = userRepository.findByUserId(userId);
        if(user == null){
            log.info("존재하지 않는 유저입니다.");
            return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
        }
        if(!user.getUserId().equals(getCurrentUserId())){
            log.info("다른 유저를 탈퇴시킬 수 없습니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        userRepository.deleteById(userId);
        log.info("정상 탈퇴되었습니다.");
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<?> findUserId(FindUserIdReqDto findUserIdReqDto) {

        Users user = userRepository.findUserIdByEmailIdAndEmailDomain(findUserIdReqDto.getEmailId(), findUserIdReqDto.getEmailDomain());

        if (user != null) {
            return new ResponseEntity<>(user.getUserId(), HttpStatus.OK);
        }
        log.info("존재하지 않는 유저입니다.");
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

    }

    @Transactional
    public ResponseEntity<?> giveUserTempPass(GiveTempPwReqDto giveTempPwReqDto) {
        Users user = userRepository.findByUserIdAndEmailIdAndEmailDomain(giveTempPwReqDto.getUserId(), giveTempPwReqDto.getEmailId(), giveTempPwReqDto.getEmailDomain());
        if (user != null) {
            // ASCII 범위 – 영숫자(0-9, a-z, A-Z)
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

            // 랜덤 비밀번호 생성 (10자리)
            SecureRandom random = new SecureRandom();
            String newPwd = IntStream.range(0, 10)
                    .map(i -> random.nextInt(chars.length()))
                    .mapToObj(randomIndex -> String.valueOf(chars.charAt(randomIndex)))
                    .collect(Collectors.joining());

            // DB에 새비밀번호 업데이트
            user.updatePassword(passwordEncoder.encode(newPwd));

            // 메일 전송
            mailService.sendMailWithNewPwd(user.getEmailId() + "@" + user.getEmailDomain(), newPwd);
            log.info("정상적으로 메일이 전송되었습니다.");
            return new ResponseEntity<>(true, HttpStatus.OK);
        }
        log.info("존재하지 않는 유저입니다.");
        return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
    }

    @Transactional
    public ResponseEntity<?> availableNickname(String nickname) {
        Users user = userRepository.findByNickname(nickname);
        if (user != null) {
            log.info("이미 존재하는 닉네임입니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        log.info("사용 가능한 닉네임입니다.");
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<?> evalUser(EvalUserReqDto evalUserReqDto) {
        Users user = userRepository.findByUserId(evalUserReqDto.getUserId());
        if (user == null) {
            log.info("존재하지 않는 유저입니다.");
            return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
        } else if (evalUserReqDto.getUserId().equals(getCurrentUserId())) {
            log.info("자신을 평가할 수 없습니다.");
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }

        int dp;
        if (evalUserReqDto.getEvalType().equals(EvalType.GOOD)) {
            dp = 10;
        } else if (evalUserReqDto.getEvalType().equals(EvalType.BAD)) {
            dp = -15;
        } else {
            dp = 0;
        }
        user.updatePoint(dp);
        log.info("정상적으로 평가되었습니다.");
        return new ResponseEntity<>(true, HttpStatus.OK);
    }
}
