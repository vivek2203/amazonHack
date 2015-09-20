package com;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;


import com.opencsv.CSVReader;

public class FileListener  {

	
	HashMap<String, HashMap> cacheMap1 = new HashMap<String, HashMap>();
	HashMap<String, HashMap> notificationMap = new HashMap<String, HashMap>();

	public void listenerCSV() {
		System.out.println("in listerenr func"); //$NON-NLS-1$
		String sharedPath = Messages.getString("FileListener.LISTENER_PATH"); //$NON-NLS-1$
		WatchService watcher = null;
		HashMap<String, HashMap> outerMap = new HashMap<String, HashMap>();
		Path dir = Paths.get(sharedPath);

		try {

			watcher = FileSystems.getDefault().newWatchService();
			WatchKey key = dir.register(watcher, ENTRY_MODIFY);// ,
																// ENTRY_DELETE,ENTRY_MODIFY);
			// }
		} catch (IOException x) {
			System.err.println(x);
		}
		for (;;) {

			// wait for key to be signaled
			WatchKey key;
			try {

				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();

				// This key is registered only
				// for ENTRY_CREATE events,
				// but an OVERFLOW event can
				// occur regardless if events
				// are lost or discarded.
				if (kind == OVERFLOW) {
					continue;
				}

				// The filename is the // context of the event.
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();

				try {
					Path child = dir.resolve(filename);
					// Files.probeContentType(child) check null

					if (child != null
							&& !Messages.getString("FileListener.FILE_EXTN").equals(Files.probeContentType(child))) { //$NON-NLS-1$
						System.out.format("New file '%s'" //$NON-NLS-1$
								+ " is a csv file .%n", filename); //$NON-NLS-1$

						outerMap.clear();
					
					
						readCSVFile(child,outerMap);												
						
				
			
						
						continue;
					}
				} catch (IOException x) {
					System.err.println(x);
					continue;
				}
			}

			// Reset the key -- this step is critical if you want to
			// receive further watch events. If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			boolean valid = key.reset();
			if (!valid) {
				break;
			}
		}
	}

	public void readCSVFile(Path child,HashMap<String, HashMap> outerMap) {
		long time = System.nanoTime();

		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(child.toString()));

			String[] nextLine;
			HashMap<String, String> innerMap = null;
			int count = 0;
			String oldValue = null;
			while ((nextLine = reader.readNext()) != null) {

				if (count == 0
						|| (!oldValue.equals(nextLine[0]) && oldValue != null)) {
					innerMap = new HashMap<String, String>();
					outerMap.put(nextLine[0], innerMap);

				}
				innerMap.put(nextLine[1], nextLine[2]);
				oldValue = nextLine[0];
				count++;

			}

			buildNotification(outerMap, cacheMap1);
			System.out.println("outerMAP" + outerMap); //$NON-NLS-1$
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		time = System.nanoTime() - time;
		System.out.println(time);

	}

	@SuppressWarnings("unchecked")
	public void buildNotification(HashMap outerMap, HashMap cacheMap1) {
		int diffInt = 0;
		String newValue=null;
		notificationMap.clear();
		System.out.println("OuterMap" + outerMap); //$NON-NLS-1$
		HashMap<String, String> outerMapTemp = new HashMap<String, String>();
		HashMap<String, String> cacheMapTemp = new HashMap<String, String>();
		
		if(!cacheMap1.isEmpty()){
		for (Object key1 : outerMap.keySet()) {

			if (cacheMap1.containsKey(key1)) {

				outerMapTemp = (HashMap<String, String>) outerMap.get(key1);
				cacheMapTemp = (HashMap<String, String>) cacheMap1.get(key1);

				if (outerMapTemp.get(Messages.getString("FileListener.USER_FIELD_PRICE")) != null //$NON-NLS-1$
						&& cacheMapTemp.get(Messages.getString("FileListener.USER_FIELD_PRICE")) != null) { //$NON-NLS-1$
					diffInt = Integer.parseInt(outerMapTemp.get(Messages.getString("FileListener.USER_FIELD_PRICE")) //$NON-NLS-1$
							.toString())
							- Integer.parseInt(cacheMapTemp.get(Messages.getString("FileListener.USER_FIELD_PRICE")) //$NON-NLS-1$
									.toString());
				
					System.out.println("diffInt" + diffInt); //$NON-NLS-1$
					if (diffInt != 0) {
						
						//update new value
						newValue=outerMapTemp.get(Messages.getString("FileListener.USER_FIELD_PRICE")); //$NON-NLS-1$
						cacheMapTemp.put(Messages.getString("FileListener.USER_FIELD_PRICE"),newValue); //$NON-NLS-1$
						cacheMap1.put(key1,cacheMapTemp);
					
						// user condition
						if ((diffInt > 100 || diffInt < -99))
						{
							notificationMap.put(key1.toString(), outerMapTemp);
						}
					}

				}

			}else{
				System.out.println("Key not found"+cacheMap1); //$NON-NLS-1$
				cacheMap1.put(key1, outerMap.get(key1));
			}
		
		}
		
		
		}
		else{// key not found in cache
				System.out.println("Copy Key not found"+cacheMap1); //$NON-NLS-1$
				cacheMap1=(HashMap) outerMap.clone();
		}

		

		writeCSV(notificationMap);
		
		System.out.println("cacheMap" + cacheMap1); //$NON-NLS-1$
		System.out.println("notificationMap" + notificationMap); //$NON-NLS-1$
	}
	
	
	public void writeCSV(HashMap<String,HashMap> notifyMap){
		
		try {
		
			if(notifyMap.isEmpty()){
				return;
			}
			
			FileWriter writer = new FileWriter(Messages.getString("FileListener.NOTIFY_LISTENER_FILE"));
			for (String key : notifyMap.keySet()) {
		    	writer.append(key);
		    	writer.append(',');
		    	writer.append(Messages.getString("FileListener.USER_FIELD_PRICE"));
			    writer.append(',');
			    writer.append(notifyMap.get(key).get(Messages.getString("FileListener.USER_FIELD_PRICE")).toString());
			    writer.append(',');
			    writer.append("amazonhacktest@gmail.com");
			    writer.append('\n');
			}
		    
				
		    //generate whatever data you want
				
		    writer.flush();
		    writer.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

	public static void main(String[] args) throws IOException,
			InterruptedException {
		// long time=System.nanoTime();
		FileListener flistener = new FileListener();
		flistener.listenerCSV();
		// time=System.nanoTime()-time;
		// System.out.println(time);

	}
}