package k.nutriguard.repository

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.realtime.Realtime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object SupabaseProvider {
    val client = createSupabaseClient(
            supabaseUrl = "https://espbgyoncikddqeaaxpj.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVzcGJneW9uY2lrZGRxZWFheHBqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI1MTU4NDQsImV4cCI6MjA3ODA5MTg0NH0.zq-FU-2a6Fy4Jo31LZ--npbibBZrMl_nRQZ-9CzfxQ8"
        ) {
            requestTimeout = 30.seconds
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
}