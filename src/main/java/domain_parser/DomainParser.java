package domain_parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.net.ssl.SSLSession;

import org.apache.hc.client5.*;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.util.concurrent.locks.* ;

public class DomainParser {
	Byte	IP[]	;
	Integer	Mask	,
			Port	,
			Threads	;
	Thread[] threads = null ;
	static Map<String, String[]> ip_dns ;
	static Lock lock ;
	
	static
	{
		ip_dns = new HashMap<>();
	}
	
	public DomainParser( String ip , Integer port , Integer mask , Integer th)
	{
		IP		= Arrays.asList(ip.split("\\.")).stream().map(x->Integer.valueOf(x)).map(x->(byte)x.intValue()).toArray(Byte[]::new) ;
		Port	= port					;
		Mask	= 32 - mask				;
		Threads	= th					;
	}
	
	public DomainParser( )
	{
		IP		= null	;
		Port	= 0		;
		Mask	= 0		;
		Threads	= 0		;
	}
	
	public void setNewValue(String ip , Integer port , Integer mask , Integer th)
	{
		IP		= Arrays.asList(ip.split("\\.")).stream().map(x->Integer.valueOf(x)).map(x->(byte)x.intValue()).toArray(Byte[]::new) ;
		Port	= port					;
		Mask	= 32 - mask				;
		Threads	= th					;
	}
	
	static void findDomains(Byte[] IP , Integer Port, Integer count)
	{
		System.out.println(String.join(".",Stream.of(IP).map(x->String.valueOf(((int)x)&0xff)).toArray(String[]::new)));
		Map<String, String[]> ip_dns_local = new HashMap<>(); ;
		Set<String> dns = new HashSet<>() ;
	        try (CloseableHttpClient httpclient = HttpClients.custom()
	                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
	                		.setTlsConfigResolver(x -> {return TlsConfig.DEFAULT; })
	                		.setDefaultConnectionConfig(ConnectionConfig.custom()
	                                .setConnectTimeout(Timeout.ofSeconds(1))
	                                .setSocketTimeout(Timeout.ofSeconds(1))
	                                .setValidateAfterInactivity(TimeValue.ofSeconds(1))
	                                .setTimeToLive(TimeValue.ofSeconds(1))
	                                .build())
	                        .setDefaultTlsConfig(TlsConfig.custom()
	                                .setHandshakeTimeout(Timeout.ofSeconds(1))
	                                .setSupportedProtocols(TLS.V_1_3)
	                                .build())
	                		.build())
	                .addResponseInterceptorLast(new HttpResponseInterceptor()
	                {
						@Override
						public void process(HttpResponse response, EntityDetails entity, HttpContext context)
								throws HttpException, IOException {
							SSLSession ssl = (SSLSession)context.getAttribute(HttpCoreContext.SSL_SESSION);
							if(ssl != null)
							{
								for( Certificate cert : ssl.getPeerCertificates())
								{
									try {
										((X509Certificate)cert).getSubjectAlternativeNames().forEach(x->dns.add((Integer)x.get(0) == 2 ? (String)x.get(1) : "") );
									}
									catch(Exception ex)
									{
									}
								}
							}
							
						}
	                })
	                .build()) {
	        		
	        		ClassicHttpRequest request ;
	        		
	        		for(int i = 0 ; i < count ; ++i)
	        		{
	        			try
	        			{
	        				request = ClassicRequestBuilder.get()
	        						.setHttpHost(new HttpHost(InetAddress.getByName(String.join(".",Stream.of(IP).map(x->String.valueOf(((int)x)&0xff)).toArray(String[]::new))), Port))
	        						.build();
	        				httpclient.execute(request, x->x);
	        			}
	        			catch(Exception ex)
	        			{
	        				if (Thread.interrupted()) {
	        				    return ;
	        				}
	        				System.out.println(ex.getMessage());
	        			}

	                	ip_dns_local.put(String.join(".",Stream.of(IP).map(x->String.valueOf(((int)x)&0xff)).toArray(String[]::new)), dns.toArray(new String[0]));
	                	dns.clear();
	                	getIPWithOffset(IP, 1);
	        		}
	        		lock.lock();
	        		try
	        		{
	        			ip_dns.putAll(ip_dns_local);
	        		}
	        		finally
	        		{
	        			lock.unlock();
	        		}
	        }
	        catch( Exception ex)
	        {
	        	System.out.println(ex.getMessage());
	        }
	}
	
	static void getIPWithOffset(Byte[] IP , Integer offset)
	{
		int ip = 0 ;
		
		for( int i = 0 ; i < 4 ; ++i)
			ip = ip | ( ((IP[3 - i] & 0xff) << (i * 8)) );
		
		ip += offset ;
		
		for( int i = 0 ; i < 4 ; ++i)
			IP[3 - i] = (byte)((ip >> (i * 8)) & 0xFF);
	}
	
	static Byte[] getNewIPWithOffset(Byte[] IP , Integer offset)
	{
		int ip = 0 ;
		Byte[] res = IP.clone();
		
		for( int i = 0 ; i < 4 ; ++i)
			ip = ip | ( ((res[3 - i] & 0xff) << (i * 8)) );
		
		ip += offset ;
		
		for( int i = 0 ; i < 4 ; ++i)
			res[3 - i] = (byte)((ip >> (i * 8)) & 0xFF);
		
		return res ;
	}
	
	
	public boolean getStatus()
	{
		if( threads == null)
			return true ;
		
		for( Thread th : threads)
		{
			if( th.getState() != Thread.State.TERMINATED )
				return false ;
		}
		
		return true ;
	}
	
	public boolean stopThreads()
	{
		for( Thread th : threads)
		{
			th.interrupt();
		}
		
		return getStatus() ;
	}
	
	static void writeToFile(Map<String, String[]> map, Thread[] threads)
	{
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("DNS_NAMES.txt"), StandardCharsets.UTF_8 )) {
			
			for(int i = 0 ; i < threads.length -1 ; ++i )
				threads[i].join();
			
			writer.flush();
			map.forEach((ip, dns_names) -> {
				try
				{
					writer.write(ip, 0, ip.length());
					writer.write(": ", 0, ": ".length());
					for( String s : dns_names )
					{
						String toWrite = s + ",";
						writer.write(toWrite, 0, toWrite.length());
					}
					
					writer.write("\n", 0, "\n".length());
					
				} catch (Exception x) {
					if (Thread.interrupted()) {
					    return ;
					}
				    System.err.format(x.getMessage());
				}
			});
		    
		} catch (Exception x) {
			if (Thread.interrupted()) {
			    return ;
			}
		    System.err.format( x.getMessage());
		}
	}
	
	public void startProcess()
	{
		ip_dns.clear();
		lock = new ReentrantLock();
		threads = new Thread[Threads + 1] ;
		double works = Math.pow(2 , Mask);
		double perThread = works / Threads;
		
		System.out.println("Running program with " + Threads + " threads and with " + (int)perThread + " ips per thread.");
		
		for(int i = 0 ; i < Threads; ++i)
		{
			int aa = (int)(i * perThread);
			
			if(i == Threads - 1)
				threads[i] = new Thread(new Runnable() {
					final int		ipOffset = aa ;
					final int		threadElems = (int)(works - (int)perThread * (Threads - 1)) ;
					@Override
					public void run() {
						DomainParser.findDomains( getNewIPWithOffset( IP, ipOffset ) , Port, threadElems) ;
					}
				});
			else
				threads[i] = new Thread(new Runnable() {
					final int		ipOffset = aa ;
					final int		threadElems = (int)perThread ;
					@Override
					public void run() {
						DomainParser.findDomains( getNewIPWithOffset( IP, ipOffset ) , Port, threadElems) ;
					}
				});
		}
		
		threads[Threads] = new Thread(new Runnable() {
			@Override
			public void run() {
				writeToFile(ip_dns, threads) ;
			}
		});
		
		for( Thread t : threads)
			t.start();
	}
}
