package com.board2notes.app.data.remote

/**
 * Placeholder for the future FastAPI backend.
 *
 * When the backend is ready, define the API surface here, e.g. with Retrofit:
 *
 * interface BoardApi {
 *     @Multipart
 *     @POST("/enhance")
 *     suspend fun enhance(@Part image: MultipartBody.Part): EnhanceResponseDto
 *
 *     @Multipart
 *     @POST("/ocr")
 *     suspend fun ocr(@Part image: MultipartBody.Part): OcrResponseDto
 *
 *     @Multipart
 *     @POST("/visual-note")
 *     suspend fun visualNote(@Part image: MultipartBody.Part): VisualNoteResponseDto
 * }
 *
 * Then create RemoteBoardRepository(api): BoardRepository that maps DTOs to
 * domain models, and register it in AppModule instead of MockBoardRepository.
 */
object ApiConfig {
    // Android emulator → host machine localhost is 10.0.2.2
    const val BASE_URL_EMULATOR = "http://10.0.2.2:8000/"
    const val BASE_URL_PRODUCTION = "https://api.board2notes.example.com/"
}
