package com.ttcsn.venue.exception;

import com.ttcsn.common.exception.NotFoundException;

public class RoomNotFoundException extends NotFoundException {
    public RoomNotFoundException(Long roomId) {
        super("Không tìm thấy phòng chiếu với roomId = " + roomId);
    }
}
