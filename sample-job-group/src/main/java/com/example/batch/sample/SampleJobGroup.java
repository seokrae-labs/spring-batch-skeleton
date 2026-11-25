package com.example.batch.sample;

import com.example.batch.maindb.MainDbConfig;
import org.springframework.context.annotation.Import;

@Import({
    MainDbConfig.class,
    SampleJobConfig.class
})
public class SampleJobGroup {
}
