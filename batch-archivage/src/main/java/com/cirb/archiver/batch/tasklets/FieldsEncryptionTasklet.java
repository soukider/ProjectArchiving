
package com.cirb.archiver.batch.tasklets;

import com.cirb.archiver.batch.utils.JavaPGP;
import com.cirb.archiver.domain.Consumer;
import com.cirb.archiver.domain.JsonArchive;
import com.cirb.archiver.domain.Provider;
import com.cirb.archiver.repositories.ConsumerRepository;
import com.cirb.archiver.repositories.ProviderRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.SecretKey;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FieldsEncryptionTasklet implements Tasklet {

	private static final String KEYSTORE_ENTRY_ALIAS = "field_encryption_secret";
	private static final String KEYSTORE_TYPE = "JCEKS";
	private static final String KEYSTORE_FILE_NAME = "SecretKeyStore.jks";

	@Autowired
	private ConsumerRepository consumerRepository;
	@Autowired
	private ProviderRepository providerRepository;

	private ItemWriter<JsonArchive> itemWriter;

	private String jksPassword;

	private JavaPGP encryptor;

	private String jksFilepath;

	// @Autowired
	public FieldsEncryptionTasklet(ConsumerRepository consumerRepository, ProviderRepository providerRepository,
			ItemWriter<JsonArchive> itemWriter, String jksPassword, String jksFilepath)
			throws NoSuchAlgorithmException {
		this.consumerRepository = consumerRepository;
		this.providerRepository = providerRepository;
		this.itemWriter = itemWriter;
		encryptor = JavaPGP.getInstance();
		this.jksPassword = jksPassword;
		this.jksFilepath = jksFilepath;
	}

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		Calendar oneYearAgo = Calendar.getInstance();
		oneYearAgo.set(oneYearAgo.get(Calendar.YEAR) - 1, oneYearAgo.get(Calendar.MONTH),
				oneYearAgo.get(Calendar.DATE));
		List<Consumer> consumers = consumerRepository
				.findByExternalTimestampLessThanEqualOrderById(oneYearAgo.getTime());
		List<Provider> providers = providerRepository
				.findByExternalTimestampLessThanEqualOrderById(oneYearAgo.getTime());
		List<JsonArchive> archives = new ArrayList<>();
		saveEncryptionKey();

		consumers.stream().limit(100).forEach(consumer -> {
			providers.stream().forEach(provider -> {
				if (consumer.getTransactionId() != null
						&& consumer.getTransactionId().equals(provider.getTransactionId())) {
					JsonArchive archive = null;
					try {
						Consumer c = consumer.isEncrypted() ? consumer : encryptConsumerFields(consumer);
						Provider p = provider.isEncrypted() ? provider : encryptProviderFields(provider);
						archive = new JsonArchive(new Date(), c, p);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (GeneralSecurityException e) {
						e.printStackTrace();
					}
					archives.add(archive);
				}
			});
		});
		this.itemWriter.write(archives);
		return RepeatStatus.FINISHED;
	}

	private Consumer encryptConsumerFields(Consumer consumer)
			throws UnsupportedEncodingException, GeneralSecurityException {
		if (consumer.getInstitute() != null)
			consumer.setInstitute(encryptor.encrypt(consumer.getInstitute()));

		if (consumer.getLegalContext() != null)
			consumer.setLegalContext(encryptor.encrypt(consumer.getLegalContext()));
		consumer.setEncrypted(true);
		return consumer;
	}

	private Provider encryptProviderFields(Provider provider)
			throws UnsupportedEncodingException, GeneralSecurityException {
		if (provider.getInstitute() != null)
			provider.setInstitute(encryptor.encrypt(provider.getInstitute()));
		if (provider.getLegalContext() != null)
			provider.setLegalContext(encryptor.encrypt(provider.getLegalContext()));
		provider.setEncrypted(true);
		return provider;
	}

	private void saveEncryptionKey()
			throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {

		KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
		keyStore.load(null, jksPassword.toCharArray());
		SecretKey key = encryptor.getAESKey();
		KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(key);
		KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(jksPassword.toCharArray());
		keyStore.setEntry(KEYSTORE_ENTRY_ALIAS, secretKeyEntry, protectionParameter);
		try (FileOutputStream fos = new FileOutputStream(this.jksFilepath + KEYSTORE_FILE_NAME)) {
			keyStore.store(fos, jksPassword.toCharArray());
		}
	}

}
