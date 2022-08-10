package com.ssafy.five.controller;

import com.ssafy.five.controller.dto.req.BlockReqDto;
import com.ssafy.five.domain.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/block")
public class BlockController {

    private final BlockService blockService;

    @PostMapping
    public ResponseEntity<?> addBlock(@RequestBody BlockReqDto blockReqDto) {
        Map<String, Integer> response = blockService.addBlock(blockReqDto);
        return new ResponseEntity<>(response.get("result").equals(200), HttpStatus.valueOf(response.get("result")));
    }

    @GetMapping
    public ResponseEntity<?> findAllBlockList(@RequestBody String userId) {
        Map<String, ?> blocker = blockService.findAllBlockList(userId);
        return new ResponseEntity<>(blocker.get("result"), blocker.get("result").equals(false)? HttpStatus.UNAUTHORIZED : HttpStatus.OK);
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<?> deleteBlock(@PathVariable Long blockId) {
        Map<String, Boolean> response = blockService.deleteBlock(blockId);
        return new ResponseEntity<>(response.get("result"), response.get("result")? HttpStatus.OK : HttpStatus.CONFLICT);
    }
}