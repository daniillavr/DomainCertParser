package main;

import domain_parser.*;
import rest_api.*;

public class Main
{
	static public void main(String[] args)
	{
		DomainParser dp = new DomainParser() ;
		//dp.splitByThreads();
		
		RestAPI ra = new RestAPI( dp );
	}
}
