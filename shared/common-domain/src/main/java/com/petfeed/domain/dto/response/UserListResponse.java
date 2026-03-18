package com.petfeed.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponse {

    private List<UserSummary> users;
    private Long nextCursor;
    private boolean hasNext;
}
