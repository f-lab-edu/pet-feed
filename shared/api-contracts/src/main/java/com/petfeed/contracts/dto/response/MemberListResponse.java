package com.petfeed.contracts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class MemberListResponse {

    @com.fasterxml.jackson.annotation.JsonProperty("users")
    private List<MemberSummary> members;
    private Long nextCursor;
    private boolean hasNext;
}
