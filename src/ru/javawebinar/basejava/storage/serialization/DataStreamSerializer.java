package ru.javawebinar.basejava.storage.serialization;

import ru.javawebinar.basejava.model.*;

import java.io.*;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataStreamSerializer implements StreamSerializer {

    @Override
    public void toWrite(OutputStream os, Resume resume) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(os)) {
            dos.writeUTF(resume.getUuid());
            dos.writeUTF(resume.getFullName());
            writeCollection(dos, (dataOutputStream) -> {
                Map<ContactType, String> contacts = resume.getContacts();
                dos.writeInt(contacts.size());
                for (Map.Entry<ContactType, String> entry : contacts.entrySet()) {
                    dos.writeUTF(entry.getKey().name());
                    dos.writeUTF(entry.getValue());
                }
            });
            writeCollection(dos, (dataOutputStream) -> {
                Map<SectionType, Section> sections = resume.getSections();
                dos.writeInt(sections.size());
                for (Map.Entry<SectionType, Section> entry : sections.entrySet()) {
                    dos.writeUTF(entry.getKey().name());
                    SectionType sectionType = entry.getKey();
                    switch (sectionType) {
                        case OBJECTIVE:
                        case PERSONAL:
                            dos.writeUTF(((TextSection) entry.getValue()).getSectionContent());
                            break;
                        case ACHIEVEMENT:
                        case QUALIFICATIONS:
                            writeCollection(dos, (dataOutputStream1) -> {
                                List<String> listSectionContent = ((ListSection) entry.getValue()).getSectionContent();
                                int sizeList = listSectionContent.size();
                                dos.writeInt(sizeList);
                                for (String aListSectionContent : listSectionContent) {
                                    dos.writeUTF(aListSectionContent);
                                }
                            });
                            break;
                        case EXPERIENCE:
                        case EDUCATION:
                            writeCollection(dos, (dataOutputStream1Scope) -> {
                                List<Company> companies = ((CompanySection) entry.getValue()).getSectionContent();
                                dos.writeInt(companies.size());
                                for (Company company : companies) {
                                    dos.writeUTF(company.getCompanyName());
                                    if (company.getUrl() == null) {
                                        dos.writeUTF("");
                                    } else {
                                        dos.writeUTF(company.getUrl());
                                    }
                                    writeCollection(dos, (dataOutputStream2Scope) -> {
                                        List<Company.PositionInCompany> positionList = company.getListOfPositions();
                                        dos.writeInt(positionList.size());
                                        for (Company.PositionInCompany position : positionList) {
                                            dos.writeInt(position.getStartDate().getYear());
                                            dos.writeInt(position.getStartDate().getMonth().getValue());
                                            if (position.getEndDate() == null) {
                                                dos.writeInt(3000);
                                                dos.writeInt(1);
                                            } else {
                                                dos.writeInt(position.getEndDate().getYear());
                                                dos.writeInt(position.getEndDate().getMonth().getValue());
                                            }
                                            dos.writeUTF(position.getPosition());
                                            if (position.getFunctions() == null) {
                                                dos.writeUTF("");
                                            } else {
                                                dos.writeUTF(position.getFunctions());
                                            }
                                        }
                                    });
                                }
                            });
                            break;
                    }
                }
            });
        }
    }

    @Override
    public Resume toRead(InputStream is) throws IOException {
        try (DataInputStream dis = new DataInputStream(is)) {
            String uuid = dis.readUTF();
            String fullName = dis.readUTF();
            Resume resume = new Resume(uuid, fullName);
            readCollection(dis, (dataInputStream) -> {
                int sizeContacts = dis.readInt();
                for (int i = 0; i < sizeContacts; i++) {
                    resume.setContact(ContactType.valueOf(dis.readUTF()), dis.readUTF());
                }
            });
            readCollection(dis, (dataInputStream) -> {
                int sizeSections = dis.readInt();
                for (int i = 0; i < sizeSections; i++) {
                    String sectionType = dis.readUTF();
                    switch (sectionType) {
                        case "OBJECTIVE":
                        case "PERSONAL":
                            resume.setSection(SectionType.valueOf(sectionType), new TextSection(dis.readUTF()));
                            break;
                        case "ACHIEVEMENT":
                        case "QUALIFICATIONS":
                            readCollection(dis, (dataInputStream1Scope) -> {
                                List<String> listSectionContent = new ArrayList<>();
                                int sizeList = dis.readInt();
                                for (int point = 0; point < sizeList; point++) {
                                    listSectionContent.add(dis.readUTF());
                                    resume.setSection(SectionType.valueOf(sectionType), new ListSection(listSectionContent));
                                }
                            });
                            break;
                        case "EXPERIENCE":
                        case "EDUCATION":
                            List<Link> linkList = new ArrayList<>();
                            List<Company.PositionInCompany[]> positionArrayList = new ArrayList<>();
                            readCollection(dis, (dataInputStream1Scope) -> {
                                int sizeCompanyList = dis.readInt();
                                for (int j = 0; j < sizeCompanyList; j++) {
                                    linkList.add(new Link(dis.readUTF(), dis.readUTF()));
                                    readCollection(dis, (dataInputStream2Scope) -> {
                                        int sizePositions = dis.readInt();
                                        Company.PositionInCompany[] positionArray = new Company.PositionInCompany[sizePositions];
                                        for (int k = 0; k < sizePositions; k++) {
                                            positionArray[k] = new Company.PositionInCompany(dis.readInt(), Month.of(dis.readInt()),
                                                    dis.readInt(), Month.of(dis.readInt()), dis.readUTF(), dis.readUTF());
                                        }
                                        positionArrayList.add(positionArray);
                                    });
                                }
                                readCollection(dis, (dataInputStream2Scope) -> {
                                    List<Company> companyList = new ArrayList<>();
                                    for (int m = 0; m < sizeCompanyList; m++) {
                                        companyList.add(new Company(linkList.get(m).getName(), linkList.get(m).getUrl(), positionArrayList.get(m)));
                                    }
                                    resume.setSection(SectionType.valueOf(sectionType), new CompanySection(companyList));
                                });
                            });
                            break;
                    }
                }
            });
            return resume;
        }
    }

    interface WriteCollection {
        void write(DataOutputStream dos) throws IOException;
    }

    void writeCollection(DataOutputStream dos, WriteCollection writeFunctionalInterface) throws IOException {
        writeFunctionalInterface.write(dos);
    }

    interface ReadCollection {
        void read(DataInputStream dis) throws IOException;
    }

    void readCollection(DataInputStream dis, ReadCollection readFunctionalInterface) throws IOException {
        readFunctionalInterface.read(dis);
    }
}
