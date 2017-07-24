package com.flipkart.zjsonpatch;

import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;

public class TestOperationTest extends AbstractTest {

    @Parameterized.Parameters
    public static Collection<PatchTestCase> data() throws IOException {
        return PatchTestCase.load("test");
    }
}
