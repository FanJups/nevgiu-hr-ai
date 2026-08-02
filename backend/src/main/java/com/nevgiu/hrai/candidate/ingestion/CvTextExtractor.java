package com.nevgiu.hrai.candidate.ingestion;

import java.io.IOException;

public interface CvTextExtractor {
    String extract(byte[] content) throws IOException;
}
