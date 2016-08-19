package com.kamesuta.mc.signpic.image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.kamesuta.mc.signpic.Reference;
import com.kamesuta.mc.signpic.image.exception.InvaildImageException;

import net.minecraft.client.resources.I18n;

public class RemoteImage extends Image {
	protected ImageDownloader downloading;
	protected Thread downloadingprocess;
	protected ImageLoader loading;
	protected Thread loadingprocess;
	protected File local;

	public RemoteImage(final String id, final ImageLocation location) {
		super(id);
		this.location = location;
	}

	public void init() {
		Reference.logger.info("PreLoading Start: " + this);
		this.state = ImageState.INITALIZED;
	}

	public void download() {
		this.state = ImageState.DOWNLOADING;
		try {
			final File local = this.location.localLocation(this);

			Reference.logger.info("PreLoading/Downloading Start: " + this);
			if (local.exists()) {
				this.state = ImageState.DOWNLOADED;
			} else {
				Reference.logger.info("File not exists: " + this);
				if (this.downloading == null)
					this.downloading = new ImageDownloader(this, this.location);
				if (this.downloadingprocess == null) {
					this.downloadingprocess = new Thread(this.downloading);
					this.downloadingprocess.start();
				}
			}
		} catch (final Exception e) {
			this.state = ImageState.ERROR;
			this.advmsg = I18n.format("signpic.advmsg.unknown", e);
			Reference.logger.error("UnknownError", e);
		}
	}

	public void ioload() {
		this.state = ImageState.IOLOADING;
		try {
			final File local = this.location.localLocation(this);

			if (local.exists()) {
				this.local = local;
				Reference.logger.info("Loading: " + this);
				if (this.loading == null)
					this.loading = new ImageLoader(this, local);
				if (this.loadingprocess == null) {
					this.loadingprocess = new Thread(this.loading);
					this.loadingprocess.start();
				}
			} else {
				throw new FileNotFoundException("The file was changed");
			}
		} catch (final InvaildImageException e) {
			this.state = ImageState.ERROR;
			this.advmsg = I18n.format("signpic.advmsg.invaildimage");
		} catch (final IOException e) {
			this.state = ImageState.ERROR;
			this.advmsg = I18n.format("signpic.advmsg.io", e);
		} catch (final Exception e) {
			this.state = ImageState.ERROR;
			this.advmsg = I18n.format("signpic.advmsg.unknown", e);
			Reference.logger.error("UnknownError", e);
		}
	}

	public void textureload() {
		this.state = ImageState.TEXTURELOADING;
		ImageManager.lazyloadqueue.offer(this);
	}

	protected int processing = 0;
	@Override
	public boolean processTexture() {
		if (this.state == ImageState.TEXTURELOADING) {
			final List<ImageTexture> texs = this.texture.getAll();
			if (this.processing < texs.size()) {
				final ImageTexture tex = texs.get(this.processing);
				tex.load();
				this.processing++;
				return false;
			} else {
				this.state = ImageState.TEXTURELOADED;
				return true;
			}
		} else {
			Reference.logger.warn("Image#loadTexture only must be called TEXTURELOADING phase");
			return true;
		}
	}

	public void complete() {
		this.state = ImageState.AVAILABLE;
	}

	@Override
	public void process() {
		switch(this.state) {
		case INIT:
			init();
			break;
		case INITALIZED:
			download();
			break;
		case DOWNLOADED:
			ioload();
			break;
		case IOLOADED:
			textureload();
			break;
		case TEXTURELOADED:
			complete();
			break;
		default:
			break;
		}
	}

	@Override
	public float getProgress() {
		switch(this.state) {
		case AVAILABLE:
			return 1f;
		case DOWNLOADING:
		case FAILED:
			if (this.downloading != null)
				return this.downloading.getProgress();
		case TEXTURELOADING:
			if (this.texture != null && !this.texture.getAll().isEmpty()) {
				final float f = (float)this.processing / this.texture.getAll().size();
				return f;
			}
		default:
			return 0;
		}
	}

	@Override
	public String getStatusMessage() {
		return I18n.format(this.state.msg, (int)getProgress()*100, this.advmsg);
	}

	@Override
	public String advMessage() {
		return this.advmsg;
	}

	@Override
	public String getLocal() {
		if (this.local != null)
			return "File:"+this.local.getName();
		else
			return "None";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof RemoteImage))
			return false;
		final Image other = (Image) obj;
		if (this.id == null) {
			if (other.id != null)
				return false;
		} else if (!this.id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("RemoteImage[%s]", this.id);
	}
}
