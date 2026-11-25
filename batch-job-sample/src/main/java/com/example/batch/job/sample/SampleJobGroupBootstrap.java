package com.example.batch.job.sample;

import com.example.batch.core.BatchConfig;
import org.springframework.context.annotation.Import;

@Import({BatchConfig.class, SampleJobGroup.class})
public class SampleJobGroupBootstrap {
}
