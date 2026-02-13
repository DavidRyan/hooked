defmodule HookedApi.PubSubTopics do
  @catch_enrichment "catch_enrichment"
  @skunk_enrichment "skunk_enrichment"

  def catch_enrichment, do: @catch_enrichment
  def skunk_enrichment, do: @skunk_enrichment
end
