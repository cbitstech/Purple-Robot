require 'json'
require 'open-uri'
require 'sxp'

class PurpleRobot
  def self.config_from_json_url(url)
    json = open(url)
  	return self.config_from_hash(JSON.parse(json.read))
  end

  def self.config_from_hash(options_hash)
    options = Array.new
    
    options_hash.each do |key, value|
       if value == "True"
         value = :'#t'
       elsif value == "False"
         value = :'#f'
       end

       option = [key, :".", value]
       
       options.push(option)
    end
    
    return [:begin, [:'pr-update-config', [:quote, options]]].to_sxp
  end
end

if __FILE__ == $0
  print PurpleRobot.config_from_json_url(ARGV[0])
end