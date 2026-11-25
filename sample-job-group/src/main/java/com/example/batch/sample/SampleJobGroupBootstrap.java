package com.example.batch.sample;

import com.example.batch.support.BatchConfig;
import org.springframework.context.annotation.Import;

@Import({BatchConfig.class, SampleJobGroup.class})
public class SampleJobGroupBootstrap {
}
