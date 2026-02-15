export type Catch = {
  id: string
  species: string | null
  location: string
  latitude: number
  longitude: number
  caught_at: string | null
  notes: string | null
  image_url: string | null
  weather_data: Record<string, unknown> | null
  inserted_at: string
}
