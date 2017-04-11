package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

public class InstanceDetail extends Instance {
	
	//private Spec spec;  @TODO

	private List<Image> images = new ArrayList<Image>();

	private List<Volume> volumes = new ArrayList<Volume>();
	
	private List<Network> networks = new ArrayList<Network>();

	private List<Keypair> keypairs = new ArrayList<Keypair>();

  
	@Override
	public List<Image> getImages() {
		return images;
	}

	@Override
	public void setImages(List<Image> images) {
		this.images = images;
	}

	@Override
	public List<Volume> getVolumes() {
		return volumes;
	}

	@Override
	public void setVolumes(List<Volume> volumes) {
		this.volumes = volumes;
	}

	@Override
	public List<Network> getNetworks() {
		return networks;
	}

	@Override
	public void setNetworks(List<Network> networks) {
		this.networks = networks;
	}

	@Override
	public List<Keypair> getKeypairs() {
		return keypairs;
	}

	@Override
	public void setKeypairs(List<Keypair> keypairs) {
		this.keypairs = keypairs;
	}
	
	@Override
	public void addImage(Image image){
		this.images.add(image);
	}
	
	@Override
	public void addVolume(Volume volume){
		this.volumes.add(volume);
	}
	
	@Override
	public void addKeypair(Keypair keypair){
		this.keypairs.add(keypair);
	}
	
	@Override
	public void addNetwork(Network network){
		this.networks.add(network);
	}

}

