# Setting Up OpenAI API Key

This document explains how to properly set up and use your OpenAI API key with the Content Swiper app.

## API Key Security Best Practices

1. **Never commit your API key to version control**
2. **Never hardcode your API key in the application code**
3. **Use environment variables or secure storage for API keys**
4. **Restrict API key permissions to only what's needed**
5. **Regularly rotate your API keys**

## Setting Up Your API Key

There are three ways to provide your OpenAI API key to the app:

### 1. Using a .env File (Recommended for Development)

1. Create a file named `.env` in the root directory of the project
2. Add your OpenAI API key to the file in the following format:
   ```
   OPENAI_API_KEY=your_api_key_here
   ```
3. Make sure the `.env` file is added to your `.gitignore` to prevent it from being committed to version control

### 2. Using Environment Variables (Recommended for CI/CD)

Set the `OPENAI_API_KEY` environment variable in your system:

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY="your_api_key_here"
```

**Windows (Command Prompt):**
```cmd
set OPENAI_API_KEY=your_api_key_here
```

**macOS/Linux:**
```bash
export OPENAI_API_KEY=your_api_key_here
```

### 3. Using Android Studio Run Configuration (Alternative for Development)

1. In Android Studio, go to Run > Edit Configurations
2. Select your app configuration
3. In the "Environment Variables" field, add:
   ```
   OPENAI_API_KEY=your_api_key_here
   ```
4. Click "Apply" and "OK"

## Verifying Your API Key

After setting up your API key, you can verify it's working correctly by:

1. Running the app in debug mode
2. Checking the logcat output for messages from `ApiConfig`, `OpenAIClient`, and `ContentGenerator`
3. If the API key is valid, you should see log messages indicating successful initialization and API calls

## Troubleshooting

If you're experiencing issues with the API key:

1. **Check the logs**: Look for error messages in the logcat output
2. **Verify the API key format**: Make sure your API key starts with `sk-` and is the correct length
3. **Check environment variables**: Make sure the environment variable is set correctly
4. **Check the .env file**: Make sure the .env file exists and has the correct format
5. **Restart Android Studio**: Sometimes Android Studio needs to be restarted to pick up new environment variables
6. **Check your OpenAI account**: Make sure your API key is active and has sufficient credits

## Using the OpenAI API in the App

The app uses the [OpenAI Kotlin SDK](https://github.com/Aallam/openai-kotlin) to interact with the OpenAI API. The SDK is initialized in the `ContentSwiperApp` class and used in the `ContentGenerator` class to generate content.

The app will automatically fall back to local content generation if the API key is invalid or missing.

## API Key Rotation

It's a good practice to regularly rotate your API keys. To rotate your API key:

1. Generate a new API key in the OpenAI dashboard
2. Update your .env file or environment variable with the new key
3. Restart the app to use the new key

## Additional Resources

- [OpenAI API Documentation](https://platform.openai.com/docs/api-reference)
- [OpenAI API Keys Best Practices](https://help.openai.com/en/articles/5112595-best-practices-for-api-key-safety)
- [OpenAI Kotlin SDK Documentation](https://github.com/Aallam/openai-kotlin) 