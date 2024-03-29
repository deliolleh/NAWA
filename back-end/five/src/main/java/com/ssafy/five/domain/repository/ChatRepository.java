package com.ssafy.five.domain.repository;

import com.ssafy.five.domain.entity.Chat;
import com.ssafy.five.domain.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllByRoomId(Room room);

}
