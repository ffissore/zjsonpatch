package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class JsonPatchOptimizerTest {

    private JsonPatchOptimizer jsonPatchOptimizer;

    @Before
    public void setUp() {
        jsonPatchOptimizer = new JsonPatchOptimizer();
    }

    @Test
    public void shouldOptimizeSequenceAddRemove() throws Exception {
        JsonNode patch = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_remove.json");

        JsonNode optimized = jsonPatchOptimizer.optimize((ArrayNode) patch);
        JsonNode expectedOptimized = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_remove.optimized.json");

        assertThat(optimized, equalTo(expectedOptimized));
    }

    @Test
    public void shouldOptimizeSequenceCopyRemove() throws Exception {
        JsonNode patch = TestUtils.loadResourceAsJsonNode("/testdata/optimize/copy_remove.json");

        JsonNode optimized = jsonPatchOptimizer.optimize((ArrayNode) patch);
        JsonNode expectedOptimized = TestUtils.loadResourceAsJsonNode("/testdata/optimize/copy_remove.optimized.json");

        assertThat(optimized, equalTo(expectedOptimized));
    }

    @Test
    public void shouldOptimizeSequenceAddCopyRemove() throws Exception {
        JsonNode patch = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_copy_remove.json");

        JsonNode optimized = jsonPatchOptimizer.optimize((ArrayNode) patch);
        JsonNode expectedOptimized = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_copy_remove.optimized.json");

        assertThat(optimized, equalTo(expectedOptimized));
    }

    @Test
    public void shouldOptimizeSequenceAddMove() throws Exception {
        JsonNode patch = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_move.json");

        JsonNode optimized = jsonPatchOptimizer.optimize((ArrayNode) patch);
        JsonNode expectedOptimized = TestUtils.loadResourceAsJsonNode("/testdata/optimize/add_move.optimized.json");

        assertThat(optimized, equalTo(expectedOptimized));
    }
}
