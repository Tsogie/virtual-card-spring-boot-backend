package net.otgon.backend.mapper;

import net.otgon.backend.dto.UserDto;
import net.otgon.backend.entity.User;

public class UserMapper {

    public static User mapToUser(UserDto userDto) {

        return new User(
                userDto.getId() != null ? userDto.getId().toString() : java.util.UUID.randomUUID().toString(),
                userDto.getUsername(),
                userDto.getEmail(),
                userDto.getCreatedAt(),
                userDto.getUpdatedAt()
        );
    }
}
