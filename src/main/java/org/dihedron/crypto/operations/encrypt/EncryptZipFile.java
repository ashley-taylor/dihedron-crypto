/**
 * Copyright (c) 2012-2014, Andrea Funto'. All rights reserved. See LICENSE for details.
 */ 
package org.dihedron.crypto.operations.encrypt;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.dihedron.core.License;
import org.dihedron.core.streams.Streams;
import org.dihedron.crypto.certificates.CertificateLoader;
import org.dihedron.crypto.certificates.CertificateLoaderFactory;
import org.dihedron.crypto.exceptions.CertificateLoaderException;
import org.dihedron.crypto.exceptions.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Andrea Funto'
 */
@License
public class EncryptZipFile {
	
	private static Logger logger = LoggerFactory.getLogger(EncryptZipFile.class);
	
	@Deprecated
	public byte[] encrypt(byte[] plaintext, String provider, String url, String name, String filter) throws CryptoException{
		X509Certificate certificate = null;
		try{
			logger.info("starting encryption process...");
			Properties configuration = new Properties();
			configuration.setProperty("provider", provider);
			configuration.setProperty("ldap.url", url);			
			CertificateLoader loader = CertificateLoaderFactory.makeCertificateLoader(configuration);
			
			Properties parameters = new Properties();
			parameters.put("name", name);
			parameters.put("filter", filter);
			certificate = (X509Certificate)loader.loadCertificate(parameters);
			logger.info("certificate loaded, supports algorithm: '{}'", certificate.getPublicKey().getAlgorithm());
						  
			String[] issuerInfo = certificate.getIssuerDN().getName().split("(=|, )", -1);
			String[] subjectInfo = certificate.getSubjectDN().getName().split("(=|, )", -1);

			logger.debug("common name (CN) : '{}'", subjectInfo[3]);
			logger.debug("address          : '{}'", subjectInfo[1]);

			for (int i = 0; i < issuerInfo.length; i += 2){
				if (issuerInfo[i].equals("C")) {
					logger.debug("CountryName : '{}'", issuerInfo[i + 1]);
				}
			  	if (issuerInfo[i].equals("O")) {
			  		logger.debug("OrganizationName : '{}'", issuerInfo[i + 1]);
			  	}
			  	if (issuerInfo[i].equals("CN")) {
			  		logger.debug("CommonName : '{}'", issuerInfo[i + 1]);
			  	}
			}
			logger.info("certificate is valid from {} until {}, encrypting data...", certificate.getNotBefore(), certificate.getNotAfter());
			
			CMSTypedData message = new CMSProcessableByteArray(plaintext);
			CMSEnvelopedDataGenerator generator = new CMSEnvelopedDataGenerator();
			generator.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(certificate).setProvider("BC"));
			CMSEnvelopedData envdata = generator.generate(message, new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
			
//			String algorithm = CMSEnvelopedDataGenerator.DES_EDE3_CBC;
//			int keysize = 192;  // bits			
//			CMSEnvelopedDataGenerator fact = new CMSEnvelopedDataGenerator();
//			fact.addKeyTransRecipient((X509Certificate)certificate);
//			CMSProcessableByteArray content = new CMSProcessableByteArray(plaintext);
//			CMSEnvelopedData envdata = fact.generate(content, algorithm, keysize, "BC");
			logger.info("... processing done!");
			return envdata.getEncoded() ;
						
		} catch (CMSException e){
			 logger.error("CMS exception", e);
			 throw new CryptoException("error generating enveloped signature", e);
		} catch (IOException e) {
			 logger.error("couldn't generate enveloped signature");
			 throw new CryptoException("error generating enveloped signature", e);
//		} catch (NoSuchAlgorithmException e) {
//			logger.error("no such algorithm", e);
//			throw new CryptoException("Invalid or unsupported algorithm specified", e);
//		} catch (NoSuchProviderException e) {
//			logger.error("so such security provider", e);
//			throw new CryptoException("Error accessing security provider", e);
		} catch (CertificateLoaderException e) {
			logger.error("error loading certificate", e);
			throw new CryptoException("error loading certificate", e);
		} catch (CertificateEncodingException e) {
			logger.error("invalid certificate encoding", e);
			throw new CryptoException("invalid certificate encoding", e);
		}
	}	
	
	public void encrypt(InputStream plaintext, OutputStream encrypted, String provider, String url, String name, String filter){
				
		logger.info("starting encryption");
		
		X509Certificate certificate = null;

		boolean okCertificato=true;
		try{
			logger.info("starting encryption process...");
			Properties configuration = new Properties();
			configuration.setProperty("provider", provider);
			configuration.setProperty("ldap.url", url);			
			CertificateLoader loader = CertificateLoaderFactory.makeCertificateLoader(configuration);
						
			Properties parameters = new Properties();
			parameters.put("name", name);
			parameters.put("filter", filter);
			certificate = (X509Certificate)loader.loadCertificate(parameters);
			logger.info("certificate algorithm: " + certificate.getPublicKey().getAlgorithm());			
			  
			String[] issuerInfo = certificate.getIssuerDN().getName().split("(=|, )", -1);
			String[] subjectInfo = certificate.getSubjectDN().getName().split("(=|, )", -1);

			logger.debug("common name (CN) : " + subjectInfo[3]);
			logger.debug("address          : " + subjectInfo[1] + "\n");

			for (int i = 0; i < issuerInfo.length; i += 2){
				if (issuerInfo[i].equals("C"))
					logger.debug("CountryName : " + issuerInfo[i + 1]);
			  	if (issuerInfo[i].equals("O"))
			  		logger.debug("OrganizationName : " + issuerInfo[i + 1]);
			  	if (issuerInfo[i].equals("CN"))
			  		logger.debug("CommonName : " + issuerInfo[i + 1]);
			}
			logger.info("valid from: " + certificate.getNotBefore()+ " until: " + certificate.getNotAfter());
		} catch(Exception e){
			logger.error("couldn't instantiate X.509 certificate. ", e);
			okCertificato=false;
		}
		
		if (!okCertificato){
			logger.info("no certificate, ending process");
			return;
		}
		try{
			logger.info("encrypting data");
			 
			CMSEnvelopedDataStreamGenerator edGen = new CMSEnvelopedDataStreamGenerator();
			edGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(certificate).setProvider("BC"));  
			OutputStream out = edGen.open(encrypted, new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
			Streams.copy(plaintext, out);
		    out.close();	
		    
		} catch(CMSException ex){
			 logger.error("CMSException: ", ex.getUnderlyingException());
		} catch (IOException e) {
			 logger.error("couldn't generate enveloped signature");
		} catch (CertificateEncodingException e) {
			logger.error("certificate encoding error", e);
//		} catch (OperatorCreationException e) {
//			logger.error("operator creation error", e);
		}
		
		logger.info("encryption ending");
	}
}
