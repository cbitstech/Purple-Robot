
/**
 * Module dependencies.
 */

var express = require('express')
  , consolidate = require('consolidate')
  , http = require('http')
  , path = require('path')
  , routes = require('./routes')
  , user = require('./routes/user')
  , api = require('./routes/api');

var app = express();

app.engine('.html', consolidate.swig);

app.configure(function(){
  app.set('port', process.env.PORT || 3000);
  app.set('views', __dirname + '/views');
  app.set('view engine', 'html');
  app.set('view options', { layout: false });
  app.use(express.favicon());
  app.use(express.logger('dev'));
  app.use(express.bodyParser());
  app.use(express.methodOverride());
  app.use(express.cookieParser('your secret here'));
  app.use(express.session());
  app.use(app.router);
  app.use(express.static(path.join(__dirname, 'public')));
});


app.configure('development', function(){
  app.use(express.errorHandler());
});

app.get('/', routes.index);
app.get('/users', user.list);

api.loadRoutes(app, api);

http.createServer(app).listen(app.get('port'), function(){
  console.log("Express server listening on port " + app.get('port'));
});
