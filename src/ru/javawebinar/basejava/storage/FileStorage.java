package ru.javawebinar.basejava.storage;

import ru.javawebinar.basejava.exception.StorageException;
import ru.javawebinar.basejava.model.Resume;
import ru.javawebinar.basejava.storage.serialization.StreamSerializer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileStorage extends AbstractStorage<File> {

    private File directory;
    private StreamSerializer serializer;

    protected FileStorage(File directory, StreamSerializer serializer) {
        Objects.requireNonNull(directory, "directory must not be null");
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory.getAbsolutePath() + " is not a directory");
        }
        if (!directory.canRead() || !directory.canWrite()) {
            throw new IllegalArgumentException(directory.getAbsolutePath() + " is not readable/writeable");
        }
        this.directory = directory;
        this.serializer = serializer;
    }

    @Override
    protected void toSave(File file, Resume resume) {
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new StorageException("file create error " + file.getAbsolutePath(), file.getName(), e);
        }
        toUpdate(file, resume);
    }

    @Override
    protected Resume toGet(File file) {
        try {
            return serializer.toRead(new BufferedInputStream(new FileInputStream(file)));
        } catch (IOException e) {
            throw new StorageException("file read error", file.getName(), e);
        }
    }

    @Override
    protected void toUpdate(File file, Resume resume) {
        try {
            serializer.toWrite(new BufferedOutputStream(new FileOutputStream(file)), resume);
        } catch (IOException e) {
            throw new StorageException("file write error", resume.getUuid(), e);
        }
    }

    @Override
    protected void toDelete(File file) {
        if (!file.delete()) {
            throw new StorageException("file delete error", file.getName());
        }
    }

    @Override
    protected File getKey(String uuid) {
        return new File(directory, uuid);
    }

    @Override
    protected boolean isKeyExist(File file) {
        if (file.isFile()) {
            return file.exists();
        } else {
            return false;
        }
    }

    @Override
    protected List<Resume> getAsList() {
        File[] files = directory.listFiles();
        if (files == null) {
            throw new StorageException("directory read error");
        }
        List<Resume> resumes = new ArrayList<>();
        for (File f : files) {
            resumes.add(toGet(f));
        }
        return resumes;
    }

    @Override
    public int size() {
        String[] list = directory.list();
        if (list != null) {
            return list.length;
        } else {
            throw new StorageException("directory read error");
        }
    }

    @Override
    public void clear() {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                toDelete(f);
            }
        }
    }
}
