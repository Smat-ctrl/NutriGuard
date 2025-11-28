package db

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object TestSupabase {

    val client = createSupabaseClient(
        supabaseUrl = "https://espbgyoncikddqeaaxpj.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVzcGJneW9uY2lrZGRxZWFheHBqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI1MTU4NDQsImV4cCI6MjA3ODA5MTg0NH0.zq-FU-2a6Fy4Jo31LZ--npbibBZrMl_nRQZ-9CzfxQ8"
    ) {
        // For JVM tests, just install Postgrest (no Auth / Realtime)
        install(Postgrest)
    }
}
