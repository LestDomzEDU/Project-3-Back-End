#!/bin/bash

# Set OAuth credentials (replace with your actual values)
export GITHUB_CLIENT_ID=your_github_client_id_here
export GITHUB_CLIENT_SECRET=your_github_client_secret_here
export DISCORD_CLIENT_ID=your_discord_client_id_here
export DISCORD_CLIENT_SECRET=your_discord_client_secret_here

# Set OpenAI API Key (replace with your actual key)
export OPENAI_API_KEY=your_openai_api_key_here

# Run the Spring Boot application
./gradlew bootRun
