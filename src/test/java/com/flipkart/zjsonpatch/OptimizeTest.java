package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class OptimizeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void shouldRemoveSequenceAddRemove() throws Exception {
        JsonNode patch = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_remove.json");

        JsonNode optimized = JsonDiff.optimize((ArrayNode) patch);
        JsonNode expectedOptimized = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_remove.optimized.json");

        assertThat(optimized, equalTo(expectedOptimized));
    }

    @Test
    public void shouldRemoveSequenceCopyRemove() throws Exception {
        JsonNode patch = TestUtils.loadResourceAsJsonNode("/testdata/optimize/copy_remove.json");

        JsonNode optimized = JsonDiff.optimize((ArrayNode) patch);
        JsonNode expectedOptimized = TestUtils.loadResourceAsJsonNode("/testdata/optimize/copy_remove.optimized.json");

        assertThat(optimized, equalTo(expectedOptimized));
    }

    @Test
    public void shouldRemoveSequenceAddCopyRemove() throws Exception {
        JsonNode patch = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_copy_remove.json");

        JsonNode optimized = JsonDiff.optimize((ArrayNode) patch);
        JsonNode expectedOptimized = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_copy_remove.optimized.json");

        assertThat(optimized, equalTo(expectedOptimized));
    }
}
