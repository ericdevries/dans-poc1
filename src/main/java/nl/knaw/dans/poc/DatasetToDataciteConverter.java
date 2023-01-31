package nl.knaw.dans.poc;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import nl.knaw.dans.datacite.Affiliation;
import nl.knaw.dans.datacite.DescriptionType;
import nl.knaw.dans.datacite.Resource;
import nl.knaw.dans.datacite.ResourceType;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import nl.knaw.dans.poc.bagit.DatasetAuthor;
import nl.knaw.dans.poc.depositmanager.XPathEvaluator;
import nl.knaw.dans.poc.depositmanager.XmlReader;
import nl.knaw.dans.poc.depositmanager.XmlReaderImpl;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class DatasetToDataciteConverter {
    private static XmlReader xmlReader = new XmlReaderImpl();

    public static String convertDatasetToDataciteXml(String globalId, Path datasetXml) throws ParserConfigurationException, IOException, SAXException, JAXBException {
        var doc = xmlReader.readXmlFile(datasetXml);

        var authors = XPathEvaluator.nodes(doc, "//dcx-dai:creatorDetails/dcx-dai:author")
            .map(DatasetAuthor::parseAuthor)
            .collect(Collectors.toList());

        var description = XPathEvaluator.strings(doc, "//dc:description")
            .map(StringEscapeUtils::escapeXml10)
            .collect(Collectors.joining("; "));

        var title = XPathEvaluator.strings(doc, "//dc:title")
            .map(StringEscapeUtils::escapeXml10)
            .filter(t -> StringUtils.isNotBlank(t) && !"N/A".equals(t))
            .collect(Collectors.joining("; "));

        if (StringUtils.isBlank(title)) {
            title = ":unav";
        }

        var globalIdFormatted = globalId.substring(globalId.indexOf(':') + 1);
        //        var template = new DataCiteMetadataTemplate();
        //        template.setIdentifier(globalId.substring(globalId.indexOf(':') + 1));
        //        // TODO also add creators, but thats the same logic as authors
        //        template.setAuthors(authors);
        //        // TODO file descriptions are also included, if the provided object is a DataFile
        //        // TODO find out if DataFile is ever provided through the local submit to archive command
        //        template.setDescription(description);
        //        // TODO find out what year is being used (2023 in the example, but the dataset implies 2012 or 2013)
        //        // it either uses the publication date which is dataverse-specific, or it cannot find a date
        //        // and uses the default date (now())
        //        // it uses the first of productionDate, distributionDate, citationDate, lastUpdateTime, in that order
        //        template.setPublisherYear((1900 + new Date().getYear()) + "");
        //        // TODO this is now the name of the datastation, but in this case this should be the name of this process?
        //        template.setPublisher("PoC");
        //        template.setTitle(title);

        var resource = new Resource();

        // publisher
        var publisher = new Resource.Publisher();
        publisher.setValue("PoC");

        // the global id
        var identifier = new Resource.Identifier();
        identifier.setIdentifierType("DOI");
        identifier.setValue(globalIdFormatted);

        // authors
        var authorElements = authors.stream().map(a -> {
                var creator = new Resource.Creators.Creator();
                var name = new Resource.Creators.Creator.CreatorName();
                name.setValue(a.getDisplayName());
                creator.setCreatorName(name);

                var affiliation = new Affiliation();
                affiliation.setValue(a.getAffiliation());

                creator.getAffiliation().add(affiliation);

                return creator;
            })
            .collect(Collectors.toList());

        // authors are then turned into creators for the xml
        var creators = new Resource.Creators();
        creators.getCreator().addAll(authorElements);

        var titles = new Resource.Titles();
        var xmlTitle = new Resource.Titles.Title();
        xmlTitle.setValue(title);
        titles.getTitle().add(xmlTitle);

        var resourceType = new Resource.ResourceType();
        resourceType.setResourceTypeGeneral(ResourceType.DATASET);

        var xmlDescriptions = new Resource.Descriptions();
        var xmlDescription = new Resource.Descriptions.Description();
        xmlDescription.setDescriptionType(DescriptionType.ABSTRACT);
        xmlDescription.getContent().add(description);

        xmlDescriptions.getDescription().add(xmlDescription);

        resource.setCreators(creators);
        resource.setIdentifier(identifier);
        resource.setPublisher(publisher);
        resource.setTitles(titles);
        resource.setResourceType(resourceType);
        resource.setDescriptions(xmlDescriptions);

        var context = JAXBContext.newInstance(Resource.class);
        var marshaller = context.createMarshaller();
        var strWriter = new StringWriter();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(resource, strWriter);
        return strWriter.toString();
    }
}
