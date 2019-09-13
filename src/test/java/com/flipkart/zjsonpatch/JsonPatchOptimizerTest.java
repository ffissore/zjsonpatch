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

    private void assertPatchOptimized(String input, String expectedOutput) throws Exception {
        JsonNode patch = TestUtils.loadResourceAsJsonNode(input);

        JsonNode optimized = jsonPatchOptimizer.optimize((ArrayNode) patch);
        JsonNode expectedOptimized = TestUtils.loadResourceAsJsonNode(expectedOutput);

        assertThat(optimized, equalTo(expectedOptimized));
    }

    @Test
    public void shouldOptimizeSequenceAddRemove() throws Exception {
        assertPatchOptimized("/testdata/optimize/add_remove.json", "/testdata/optimize/add_remove.optimized.json");
    }

    @Test
    public void shouldOptimizeSequenceCopyRemove() throws Exception {
        assertPatchOptimized("/testdata/optimize/copy_remove.json", "/testdata/optimize/copy_remove.optimized.json");
    }

    @Test
    public void shouldOptimizeSequenceAddCopyRemove() throws Exception {
        assertPatchOptimized("/testdata/optimize/add_copy_remove.json", "/testdata/optimize/add_copy_remove.optimized.json");
    }

    @Test
    public void shouldOptimizeSequenceAddMove() throws Exception {
        assertPatchOptimized("/testdata/optimize/add_move.json", "/testdata/optimize/add_move.optimized.json");
    }

    @Test
    public void shouldOptimizeSequenceAddMoveRemove() throws Exception {
        assertPatchOptimized("/testdata/optimize/add_move_remove.json", "/testdata/optimize/add_move_remove.optimized.json");
    }

    @Test
    public void shouldOptimizeSequenceAddReplace() throws Exception {
        assertPatchOptimized("/testdata/optimize/add_replace.json", "/testdata/optimize/add_replace.optimized.json");
    }

    @Test
    public void shouldOptimizeComplexSequence() throws Exception {
        assertPatchOptimized("/testdata/optimize/all_together_now.json", "/testdata/optimize/all_together_now.optimized.json");
    }
}
