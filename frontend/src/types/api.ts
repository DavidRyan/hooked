export type ApiErrorPayload = {
  message?: string
}

export type LoginResponse = {
  data: {
    token: string
  }
  message?: string
}

export type CatchesResponse<TCatch> = {
  user_catches?: TCatch[]
}
