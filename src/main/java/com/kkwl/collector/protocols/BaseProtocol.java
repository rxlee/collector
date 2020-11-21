package com.kkwl.collector.protocols;

import com.kkwl.collector.common.ParsingStatusType;
import com.kkwl.collector.protocols.BaseProtocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
//基本协议 协议名称、帧解析器、缓冲区、位置、
public abstract class BaseProtocol {
	protected int id;
	protected String protocolName; //协议名称
	protected List<BaseFrameParser> parsers; //基本帧解析器列表
	protected final byte[] buffer; //每一帧数据缓冲区
	protected int pos; //位置

	protected int length;//总长度
	protected int bodyLength;//请求体长度
	protected ParsingStatusType status;//状态
	protected int toBeParsedBytesStart;//要解析的字节起始位置
	protected int toBeParsedBytesLength;//要解析的字节长度

	public BaseProtocol(int id, String protocolName) {
		this.buffer = new byte[512];
		//id
		this.id = id;
		this.protocolName = protocolName;//协议名称
		this.parsers = new ArrayList();
	}



	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getProtocolName() {
		return this.protocolName;
	}

	public void setProtocolName(String protocolName) {
		this.protocolName = protocolName;
	}

	public List<Map<String, Object>> doBytesParse(byte[] inBuffer, Map<String, Object> params) {
		return new ArrayList();
	}

	public Map<String, Object> doStringParse(LinkedBlockingQueue<String> messages, Map<String, Object> params) {
		return new HashMap();
	}

	public void resetToBeParsedBuffer() {
		this.toBeParsedBytesLength = 0;
		this.toBeParsedBytesStart = 0;
	}
}

/*
 * Location:
 * C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT
 * .jar!\BOOT-INF\classes\com\kkwl\collector\protocols\BaseProtocol.class Java
 * compiler version: 8 (52.0) JD-Core Version: 1.0.7
 */