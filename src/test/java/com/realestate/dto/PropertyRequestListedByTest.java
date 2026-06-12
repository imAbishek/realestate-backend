package com.realestate.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.dto.property.PropertyDtos.PropertyRequest;
import com.realestate.entity.Property;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the {@code listedBy} contract after AGENT was added to
 * {@link Property.ListedBy} (Owner / Promoter / Agent). The wizard sends the
 * value as a JSON string, so the real risk is request deserialization — verify
 * every allowed role binds and an unknown one is rejected.
 */
class PropertyRequestListedByTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void listedBy_agent_deserializesIntoRequest() throws Exception {
        PropertyRequest req = mapper.readValue("{\"listedBy\":\"AGENT\"}", PropertyRequest.class);
        assertThat(req.getListedBy()).isEqualTo(Property.ListedBy.AGENT);
    }

    @Test
    void listedBy_allRoles_areBindable() throws Exception {
        for (Property.ListedBy role : Property.ListedBy.values()) {
            PropertyRequest req = mapper.readValue(
                "{\"listedBy\":\"" + role.name() + "\"}", PropertyRequest.class);
            assertThat(req.getListedBy()).isEqualTo(role);
        }
    }

    @Test
    void listedBy_unknownRole_isRejected() {
        assertThatThrownBy(() ->
            mapper.readValue("{\"listedBy\":\"BROKER\"}", PropertyRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
    }
}
