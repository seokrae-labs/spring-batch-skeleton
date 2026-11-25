package com.example.batch.job.sample;

import com.example.batch.db.main.MainDbConfig;
import org.springframework.context.annotation.Import;

@Import({
    MainDbConfig.class,
    SampleJobConfig.class
})
public class SampleJobGroup {
}
