package com.kamesuta.mc.signpic.image;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Lists;
import com.kamesuta.mc.signpic.Reference;
import com.kamesuta.mc.signpic.image.exception.InvaildImageException;
import com.kamesuta.mc.signpic.lib.GifDecoder;
import com.kamesuta.mc.signpic.lib.GifDecoder.GifImage;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class ImageLoader implements Runnable {
	public static final ImageSize MAX_SIZE = new ImageSize(32, 32);

	protected Image image;
	protected InputStream input;

	public ImageLoader(final Image image, final InputStream in) throws IOException {
		this.image = image;
		this.input = in;
	}

	public ImageLoader(final Image image, final File file) throws IOException {
		this(image, new FileInputStream(file));
	}

	public ImageLoader(final Image image, final IResourceManager manager, final ResourceLocation location) throws IOException {
		this(image, manager.getResource(location).getInputStream());
	}

	@Override
	public void run() {
		try {
			final byte[] data = IOUtils.toByteArray(this.input);

			final ImageInputStream imagestream = ImageIO.createImageInputStream(new ByteArrayInputStream(data));
			final Iterator<ImageReader> iter = ImageIO.getImageReaders(imagestream);
			if (!iter.hasNext()) throw new InvaildImageException();
			final ImageReader reader = iter.next();

			if (reader.getFormatName()=="gif") {
				loadGif(data);
			} else {
				loadImage(reader, imagestream);
			}
			this.image.state = ImageState.IOLOADED;
		} catch (final InvaildImageException e) {
			this.image.state = ImageState.ERROR;
			this.image.advmsg = I18n.format("signpic.advmsg.invaildimage");
		} catch (final IOException e) {
			this.image.state = ImageState.FAILED;
			this.image.advmsg = I18n.format("signpic.advmsg.io", e);
			Reference.logger.error("IO Error", e);
		} catch (final Exception e) {
			this.image.state = ImageState.FAILED;
			this.image.advmsg = I18n.format("signpic.advmsg.unknown", e);
			Reference.logger.error("Unknown Error", e);
		}

	}

	protected static final String[] imageatt = new String[] {
			"imageLeftPosition",
			"imageTopPosition",
			"imageWidth",
			"imageHeight",
	};

	protected int maxprogress;
	protected int progress;
	public float getProgress() {
		if (this.maxprogress > 0)
			return Math.max(0, Math.min(1, (float)this.progress / this.maxprogress));
		return 0;
	}

	protected void loadGif(final byte[] data) throws IOException {
		final GifImage gifImage = GifDecoder.read(data);
		final int width = gifImage.getWidth();
		final int height = gifImage.getHeight();
		final ImageSize newsize = ImageSize.createSize(ImageSizes.LIMIT, width, height, MAX_SIZE);

		final ArrayList<ImageTexture> textures = new ArrayList<ImageTexture>();
		final int frameCount = this.maxprogress = gifImage.getFrameCount();
		for (int i = 0; i < frameCount; i++) {
			final BufferedImage image = gifImage.getFrame(i);
			final int delay = gifImage.getDelay(i);
			final ImageTexture texture = new ImageTexture(createResizedImage(image, newsize), delay);
			textures.add(texture);
			this.progress = i;
		}
		this.image.texture = new ImageTextures(textures);
	}

	protected void loadImage(final ImageReader reader, final ImageInputStream imagestream) throws IOException {
		final ImageReadParam param = reader.getDefaultReadParam();
		reader.setInput(imagestream, true, true);
		BufferedImage canvas;
		try {
			canvas = reader.read(0, param);
		} finally {
			reader.dispose();
			imagestream.close();
		}
		final ImageSize newsize = ImageSize.createSize(ImageSizes.LIMIT, canvas.getWidth(), canvas.getHeight(), MAX_SIZE);
		this.image.texture = new ImageTextures(Lists.newArrayList(new ImageTexture(createResizedImage(canvas, newsize))));
	}

	protected BufferedImage createResizedImage(final BufferedImage image, final ImageSize newsize) {
		final int wid = (int)newsize.width;
		final int hei = (int)newsize.height;
		final BufferedImage thumb = new BufferedImage(wid, hei, image.getType());
		final Graphics g = thumb.getGraphics();
		g.drawImage(image.getScaledInstance(wid, hei, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, wid, hei, null);
		g.dispose();
		return thumb;
	}
}
