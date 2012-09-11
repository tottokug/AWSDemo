package com.amazon.sds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class QueryProcesser implements Serializable {
	private static final long serialVersionUID = 1008301587224093057L;

	public static void main(String[] args) {
		File f;
		QueryProcesser qp = new QueryProcesser();
		try {
			f = File.createTempFile("java", ".ser");
			FileOutputStream fos = new FileOutputStream(f);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(qp.getMoreToken());
			oos.close();
			InputStreamReader isr = new InputStreamReader(
					new FileInputStream(f));
			int buf;
			while ((buf = isr.read()) != -1) {
				System.out.print(Integer.toHexString(buf));
				System.out.print(" ");
			}
			isr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public MoreToken getMoreToken() {
		return new MoreToken();
	}

	class MoreToken implements Serializable {
		private static final long serialVersionUID = 2899885427819876927L;

		public MoreToken() {

		}

		public MoreToken(int initialConjunctIndex, boolean isPageBoundary,
				int lastEntityID, boolean lrqEnabled,
				String queryStringChecksum, String unionIndex,
				String useQueryIndex, String consistentLSN,
				String lastAttributeValue, String sortOrder) {
			this.initialConjunctIndex = initialConjunctIndex;
			this.isPageBoundary = isPageBoundary;
			this.lastEntityID = lastEntityID;
			this.lrqEnabled = lrqEnabled;
			this.queryStringChecksum = queryStringChecksum;
			this.unionIndex = unionIndex;
			this.useQueryIndex = useQueryIndex;
			this.consistentLSN = consistentLSN;
			this.lastAttributeValue = lastAttributeValue;
			this.sortOrder = sortOrder;
		}

		int initialConjunctIndex;
		boolean isPageBoundary;
		int lastEntityID;
		boolean lrqEnabled;
		String queryStringChecksum;
		String unionIndex;
		String useQueryIndex;
		String consistentLSN;

		String lastAttributeValue;
		String sortOrder;
	}

	class Query {

	}
}
