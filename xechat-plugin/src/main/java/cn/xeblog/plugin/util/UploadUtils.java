package cn.xeblog.plugin.util;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.xeblog.commons.entity.UserMsgDTO;
import cn.xeblog.commons.entity.react.React;
import cn.xeblog.commons.entity.react.request.SendFileReact;
import cn.xeblog.commons.entity.react.result.SendFileReactResult;
import cn.xeblog.commons.enums.Action;
import cn.xeblog.commons.util.ThreadUtils;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.action.MessageAction;
import cn.xeblog.plugin.action.ReactAction;
import cn.xeblog.plugin.cache.DataCache;
import com.intellij.util.ui.ImageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.function.Consumer;

/**
 * @author anlingyi
 * @date 2020/9/10
 */
public class UploadUtils {

    private static boolean UPLOADING;
    private static final String[] ACCEPT_IMAGE_TYPE = new String[]{
            ImgUtil.IMAGE_TYPE_GIF,
            ImgUtil.IMAGE_TYPE_JPG,
            ImgUtil.IMAGE_TYPE_JPEG,
            ImgUtil.IMAGE_TYPE_BMP,
            ImgUtil.IMAGE_TYPE_PNG
    };

    private static final int MAX_SIZE = 1 << 20;

    public static void uploadImageFile(File file) {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            inputStream.mark(28);
            String fileType = FileTypeUtil.getType(inputStream);
            if (!ArrayUtil.contains(ACCEPT_IMAGE_TYPE, fileType)) {
                throw new Exception("不支持的图片类型！");
            }

            inputStream.reset();
            sendImgAsync(IOUtils.toByteArray(inputStream), generateFileName(fileType));
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleAction.showSimpleMsg(e.getMessage());
        }
    }

    public static void uploadImage(Image image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage bufferedImage = ImageUtil.toBufferedImage(image);
            ImageIO.write(bufferedImage, "png", out);
            sendImgAsync(out.toByteArray(), generateFileName("png"));
        } catch (IOException e) {
            e.printStackTrace();
            ConsoleAction.showSimpleMsg("图片上传失败！");
        }
    }

    private static String generateFileName(String fileType) {
        fileType = fileType == null ? "jpg" : fileType;
        return IdUtil.fastUUID() + "." + fileType;
    }

    private static void sendImgAsync(byte[] bytes, String fileName) {
        if (UPLOADING) {
            ConsoleAction.showSimpleMsg("请等待之前的图片上传完成！");
            return;
        }

        if (bytes.length > MAX_SIZE) {
            ConsoleAction.showSimpleMsg("图片大小不能超过" + (MAX_SIZE >> 20) + "MB！");
            return;
        }

        UPLOADING = true;
        ConsoleAction.showSimpleMsg("图片上传中...");

        SendFileReact sendFileReact = new SendFileReact();
        sendFileReact.setSize(bytes.length);
        ReactAction.request(sendFileReact, React.SEND_FILE, (Consumer<SendFileReactResult>) sendFileReactResult -> {
            try {
                if (sendFileReactResult == null) {
                    ConsoleAction.showSimpleMsg("图片上传超时！");
                    return;
                }

                if (sendFileReactResult.isAllowed()) {
                    int offset = 16 << 10;
                    int index = 0;
                    int len = bytes.length;
                    while (index < len && DataCache.isOnline) {
                        ByteBuf byteBuf = Unpooled.directBuffer();
                        byteBuf.writeInt(fileName.length());
                        byteBuf.writeBytes(fileName.getBytes());
                        byteBuf.writeInt(len);
                        byteBuf.writeInt(index);
                        byteBuf.writeBytes(bytes, index, Math.min(offset, len - index));
                        byte[] sendBytes = ByteBufUtil.getBytes(byteBuf);
                        byteBuf.release();
                        index += offset;
                        MessageAction.send(new UserMsgDTO(sendBytes, UserMsgDTO.MsgType.IMAGE), Action.CHAT);
                        ThreadUtils.spinMoment(120);
                    }
                } else {
                    ConsoleAction.showSimpleMsg(sendFileReactResult.getMsg());
                }
            } catch (Exception e) {
                ConsoleAction.showSimpleMsg("图片上传失败！");
                e.printStackTrace();
            } finally {
                UPLOADING = false;
            }
        });
    }
}
