SET envFile=".env.cmd"
SET envUriFile=".env_uris.cmd"

cd scripts\local\launch
call %envFile%
call %envUriFile%
