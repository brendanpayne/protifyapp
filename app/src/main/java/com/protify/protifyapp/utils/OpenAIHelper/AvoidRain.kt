package com.protify.protifyapp.utils.OpenAIHelper

import OpenAIResponse
import com.google.gson.GsonBuilder
import com.protify.protifyapp.APIKeys
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

class AvoidRain(RainForecast: String) {
    //Get openAI key
    val apiKey = APIKeys().getOpenAIKey()

    //Request struct
    data class Request(
        val model: String,
        val response_format: String,
        var systemContent: String,
        var userContent: String
    )
    private val model = "gpt-3.5-turbo-1106"
    private val response_format = """{ "type": "json_object" }"""

    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = GsonBuilder().create()

    private val request = okhttp3.Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "${apiKey}")
        .build()

    fun getResponse(onComplete: (String?) -> Unit) {
        //Make a new request object
        val httpPost = Request(model,
            response_format,
            "You are a helpful assistant responsible for providing estimated travel times in json format.",
            "How long does it usually take to get from Cincinnati Ohio to Miami Florida?")


        //Request body builder
        val requestBody = request.newBuilder()
            .post(
                """{
                    "model": "${httpPost.model}",
                    "response_format": ${httpPost.response_format},
                    "messages": [
                        {
                            "role": "system",
                            "content": "${httpPost.systemContent}"
                        },
                        {
                            "role": "user",
                            "content": "${httpPost.userContent}"
                        }
                    ],
                    "functions": [
                    {
                      "name": "get_travel_times",
                      "description": "Gets travel time between two locations",
                      "parameters": {
                        "type": "object",
                        "properties": {
                          "Startinglocation": {
                            "type": "string",
                            "description": "The starting location of the trip"
                          },
                          "EndingLocation": {
                            "type": "string",
                            "description": "The ending location of the trip"
                          },
                          "TotalTime": {
                            "type": "string",
                            "description": "The total time in minutes that it takes to get from the starting location to the ending location"
                          }
                        },
                        "required": ["StartingLocation", "EndingLocation", "TotalTime"]
                      }
                    }
                  ],
                  "function_call": "auto"     
                }""".trimIndent().toRequestBody()
            )
            .build()

        //Make the request
        client.newCall(requestBody).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                onComplete("Error")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (response.code == 200) {
                    val openAIResponse = gson.fromJson(body, OpenAIResponse::class.java)
                    val test = openAIResponse
                    onComplete(openAIResponse.choices[0].message.content)
                }
                else {
                    onComplete("Error")
                }
            }
        })
    }



}