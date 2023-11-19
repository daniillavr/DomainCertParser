package rest_api;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import domain_parser.DomainParser;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class RestAPI
{
	final Pattern ip = Pattern.compile("^(?:(?:00[1-9]|0[1-9]\\d|[1-9]\\d\\d|[1-9]{1,3})\\.){3}(?:00[1-9]|0[1-9]\\d|[1-9]\\d\\d|[1-9]{1,3})$");
	final Pattern mask = Pattern.compile("^(?:[1-9]|[1-2]\\d|3[0-1])$");
	final Pattern threads = Pattern.compile("^(?:[1-9]+\\d*)$");
	final Pattern port = Pattern.compile("^(?:[1-9]+\\d*)$");
	
	DomainParser dp ;
	
	public RestAPI(DomainParser domp )
	{
		dp = domp ;
		
		Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(7070);
		
		app.post("/start-process", ctx -> {
			if( !dp.getStatus() )
			{
				ctx.html("Program works, cant start again <div><form method=\"get\" action=\"/\">  <button>Back</button> </form></div>");
				return ;
			}
			
			if(		ip.matcher(ctx.formParam("ip")).find() &&
					mask.matcher(ctx.formParam("mask")).find() && 
					threads.matcher(ctx.formParam("threads")).find() &&
					port.matcher(ctx.formParam("port")).find() &&
					Integer.valueOf(ctx.formParam("port")) < 65536 &&
					Integer.valueOf(ctx.formParam("mask")) < 32)
			{
            	ctx.html("Correct fields, starting program... <div><form method=\"get\" action=\"/\">  <button>Back</button> </form></div>");
            	dp.setNewValue(ctx.formParam("ip"), Integer.valueOf(ctx.formParam("port")), Integer.valueOf(ctx.formParam("mask")), Integer.valueOf(ctx.formParam("threads")));
            	dp.startProcess();
			}
            else
            	ctx.html("Incorrect input <div><form method=\"get\" action=\"/\">  <button>Back</button> </form></div>");
        });
		
		app.post("/stop-process", ctx -> {
			if( !dp.getStatus() )
			{
				ctx.html("Stopping threads <div><form method=\"get\" action=\"/\">  <button>Back</button> </form></div>");
				dp.stopThreads();
				return ;
			}
			else
				ctx.html("Program is not working yet <div><form method=\"get\" action=\"/\">  <button>Back</button> </form></div>");
        });
		
	}
}
