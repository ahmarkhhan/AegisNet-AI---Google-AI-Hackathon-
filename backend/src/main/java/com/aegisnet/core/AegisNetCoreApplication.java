package com.aegisnet.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
    "org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration"
})
@EnableScheduling
public class AegisNetCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(AegisNetCoreApplication.class, args);
	}

}
