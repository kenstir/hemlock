require "google/analytics/data/v1beta"
require "JSON"

# Parses json file and returns the contents
def read_json_file(path)
  str = File.read(path)
  data = JSON.parse(str)
  return data
end

# Returns hash of {'iOS': i, 'Android': j}
def get_active_28d_users(app)
  root = get_repo_root()
  info_file = "#{root}/secret/#{app}-info.json"
  creds_file = "#{root}/secret/#{app}-fastlane.json"

  info = read_json_file(info_file)
  #puts("info: #{info}")

  fa_property_id = info['fa_property_id']
  #puts("id: #{fa_property_id}")

  client = ::Google::Analytics::Data::V1beta::AnalyticsData::Client.new do |config|
    config.credentials = creds_file
  end

  request = ::Google::Analytics::Data::V1beta::RunReportRequest.new(
    property: "properties/#{fa_property_id}",
    date_ranges: [
      { start_date: "yesterday", end_date: "yesterday" }
    ],
    metrics: [
      { name: "active28DayUsers" },
      #{ name: "active7DayUsers" },
      #{ name: "active1DayUsers" },
    ],
    dimensions: [
      { name: "operatingSystem" }
    ],
  )
  response = client.run_report(request)
  #puts("#{response}")

  values = {}
  if response.rows.empty?
    puts "No data available."
    return values
  end

  response.rows.each do |row|
    dimension = row.dimension_values.first.value
    value = row.metric_values.first.value.to_i
    values[dimension] = value
  end

  return values
end
