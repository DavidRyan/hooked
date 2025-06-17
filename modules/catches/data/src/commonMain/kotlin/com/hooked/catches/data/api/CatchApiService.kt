package com.hooked.catches.data.api

import com.hooked.catches.data.model.CatchDto
import com.hooked.core.domain.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CatchApiService(
    private val httpClient: HttpClient,
) {
    suspend fun getCatches(): NetworkResult<List<CatchDto>> {
        return try {
            // generate 6 mock catches with unique urls not using a loop using but using the correct types
            val remoteCatches = listOf(
                CatchDto(1L, "Salmon", 2.5, 30.0, "https://t4.ftcdn.net/jpg/03/14/68/67/240_F_314686744_dvRiiXuRg6b9EIA1a4wzadc8xEwFYi82.jpg", 0.0, 0.0, 5),
                CatchDto(2L, "Trout", 1.8, 25.0, "https://t3.ftcdn.net/jpg/01/57/08/08/240_F_157080887_PYRaLKCc3nrujv4hJjSHdxAp6yr0FI1V.jpg", 0.0, 0.0, 5),
                CatchDto(3L, "Bass", 3.2, 35.0, "https://t4.ftcdn.net/jpg/00/62/03/53/240_F_62035357_m5pfGihdCjwJK3uA6hb5zTizTboMQua2.jpg", 0.0, 0.0, 5),
                CatchDto(4L, "Pike", 4.5, 45.0, "https://t4.ftcdn.net/jpg/01/85/38/17/240_F_185381723_iLyNfQRVZg6Sk8FsZjst1pZtY6dYaPZ9.jpg", 0.0, 0.0, 5),
                CatchDto(5L, "Catfish", 5.0, 50.0, "https://t4.ftcdn.net/jpg/05/90/83/15/240_F_590831592_XR8ZVKZV85123pj6nexaqFioT57IBF8T.jpg", 0.0, 0.0, 5),
                CatchDto(6L, "Perch", 0.8, 20.0, "https://t3.ftcdn.net/jpg/01/93/84/66/240_F_193846671_yIQ6ChrpaBpq1C432ypC4VMCXoK3eRrT.jpg", 0.0, 0.0, 5),
                CatchDto(7L, "Carp", 6.5, 60.0, "https://t4.ftcdn.net/jpg/02/74/20/69/240_F_274206901_Jt1PHZTbtwne17anw5eD9oABxStNJhYT.jpg", 0.0, 0.0, 5),
            )
            NetworkResult.Success(remoteCatches)
        } catch (e: Exception) {
            NetworkResult.Error(e, "CatchApiService.getCatches")
        }
    }
    
    suspend fun getCatchDetails(catchId: Long): NetworkResult<CatchDto> {
        try {
/*
            val catchDetails = httpClient
                .get("http://10.0.2.2:8080/catches/$catchId")
                .body<CatchDto>()
*/
            val lat = 41.914043
            val long = -87.686424
            val catchDetails = when (catchId) {
                1L -> CatchDto(1L, "Salmon", 2.5, 30.0, "https://t4.ftcdn.net/jpg/03/14/68/67/240_F_314686744_dvRiiXuRg6b9EIA1a4wzadc8xEwFYi82.jpg", lat, long, 5)
                2L -> CatchDto(2L, "Trout", 1.8, 25.0, "https://t3.ftcdn.net/jpg/01/57/08/08/240_F_157080887_PYRaLKCc3nrujv4hJjSHdxAp6yr0FI1V.jpg", lat, long, 5)
                3L -> CatchDto(3L, "Bass", 3.2, 35.0, "https://t4.ftcdn.net/jpg/00/62/03/53/240_F_62035357_m5pfGihdCjwJK3uA6hb5zTizTboMQua2.jpg", lat, long, 5)
                4L -> CatchDto(4L, "Pike", 4.5, 45.0, "https://t4.ftcdn.net/jpg/01/85/38/17/240_F_185381723_iLyNfQRVZg6Sk8FsZjst1pZtY6dYaPZ9.jpg", lat, long, 5)
                5L -> CatchDto(5L, "Catfish", 5.0, 50.0, "https://t4.ftcdn.net/jpg/05/90/83/15/240_F_590831592_XR8ZVKZV85123pj6nexaqFioT57IBF8T.jpg", lat, long, 5)
                6L -> CatchDto(6L, "Perch", 0.8, 20.0, "https://t3.ftcdn.net/jpg/01/93/84/66/240_F_193846671_yIQ6ChrpaBpq1C432ypC4VMCXoK3eRrT.jpg", lat, long, 5)
                7L -> CatchDto(7L, "Carp", 6.5, 60.0, "https://t4.ftcdn.net/jpg/02/74/20/69/240_F_274206901_Jt1PHZTbtwne17anw5eD9oABxStNJhYT.jpg", lat, long, 5)
                else -> CatchDto(1L, "Salmon", 2.5, 30.0, "https://t4.ftcdn.net/jpg/03/14/68/67/240_F_314686744_dvRiiXuRg6b9EIA1a4wzadc8xEwFYi82.jpg", lat, lat, 5)
            }
            return NetworkResult.Success(catchDetails)
        } catch (e: Exception) {
            return NetworkResult.Error(e, "CatchApiService.getCatchDetails")
        }
    }
}