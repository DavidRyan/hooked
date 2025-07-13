# Test script to verify species enricher behavior
alias HookedApi.Enrichers.Species.SpeciesEnricher

# Create a mock user catch with a test image
user_catch = %{
  image_url: "https://www.inaturalist.org/attachments/local_photos/files/1/large/largemouth_bass.jpg?1234567890"
}

# Test the enricher
IO.puts("Testing species enricher...")
result = SpeciesEnricher.enrich(user_catch, %{})

IO.puts("Result: #{inspect(result)}")

if Map.has_key?(result, :species) do
  IO.puts("Species detected: #{result.species}")
  
  # Check if it's largemouth bass
  species_lower = String.downcase(result.species)
  if String.contains?(species_lower, "largemouth") and String.contains?(species_lower, "bass") do
    IO.puts("✅ VERIFIED: Species is largemouth bass")
  else
    IO.puts("❌ Species is NOT largemouth bass: #{result.species}")
  end
else
  IO.puts("❌ No species detected in result")
end