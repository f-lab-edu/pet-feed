package com.petfeed.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petfeed.contracts.dto.response.MemberListResponse;
import com.petfeed.contracts.dto.response.MemberSummary;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MemberWireCompatibilityTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void memberNamesAreUsedInJsonWithoutLegacyProperties() throws Exception {
        var response = MemberListResponse.of(List.of(MemberSummary.of("member-1", "nickname", null, true)), null, false);
        var tree = json.readTree(json.writeValueAsString(response));
        assertThat(tree.has("users")).isFalse();
        assertThat(tree.path("members")).hasSize(1);
        assertThat(tree.path("members").get(0).path("memberKey").asText()).isEqualTo("member-1");
        assertThat(tree.path("members").get(0).has("userKey")).isFalse();
    }

}
