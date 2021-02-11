package com.tool4us.net.http;



public interface IRequestHandler
{
    public String call(RequestEx req, ResponseEx res) throws Exception;
}
