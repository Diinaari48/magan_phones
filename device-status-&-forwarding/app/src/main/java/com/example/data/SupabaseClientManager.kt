package com.example.data

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientManager {
    val supabaseUrl: String = try {
        BuildConfig.SUPABASE_URL
    } catch (e: Throwable) {
        ""
    }

    val supabaseAnonKey: String = try {
        BuildConfig.SUPABASE_ANON_KEY
    } catch (e: Throwable) {
        ""
    }

    val forwardingPasscode: String = try {
        BuildConfig.FORWARDING_PASSCODE.ifBlank { "1234" }
    } catch (e: Throwable) {
        "1234"
    }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && 
                !supabaseUrl.contains("your-supabase-project") && 
                supabaseAnonKey.isNotBlank() && 
                !supabaseAnonKey.contains("your-supabase-anon-key")

    val client: SupabaseClient? by lazy {
        if (isConfigured) {
            try {
                createSupabaseClient(
                    supabaseUrl = supabaseUrl,
                    supabaseKey = supabaseAnonKey
                ) {
                    install(Postgrest)
                    install(Realtime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
}
