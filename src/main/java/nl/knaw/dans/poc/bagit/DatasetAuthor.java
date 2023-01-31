package nl.knaw.dans.poc.bagit;

import lombok.Builder;
import lombok.Data;
import nl.knaw.dans.poc.depositmanager.XPathEvaluator;
import org.w3c.dom.Node;

import static nl.knaw.dans.poc.depositmanager.IdUriHelper.reduceUriToId;
import static nl.knaw.dans.poc.depositmanager.IdUriHelper.reduceUriToOrcidId;

@Data
@Builder
public class DatasetAuthor {
    private String name;
    private String titles;
    private String initials;
    private String insertions;
    private String surname;
    private String dai;
    private String isni;
    private String orcid;
    private String lcna;
    private String role;
    private String organization;
    private String affiliation;

    static String getFirstValue(Node node, String expression) {
        return XPathEvaluator.strings(node, expression).map(String::trim).findFirst().orElse(null);
    }

    public static DatasetAuthor parseAuthor(Node node) {
        return DatasetAuthor.builder()
            .titles(getFirstValue(node, "dcx-dai:titles"))
            .initials(getFirstValue(node, "dcx-dai:initials"))
            .insertions(getFirstValue(node, "dcx-dai:insertions"))
            .surname(getFirstValue(node, "dcx-dai:surname"))
            .dai(getFirstValue(node, "dcx-dai:DAI"))
            .isni(reduceUriToId(getFirstValue(node, "dcx-dai:ISNI")))
            .orcid(reduceUriToOrcidId(getFirstValue(node, "dcx-dai:ORCID")))
            .role(getFirstValue(node, "dcx-dai:role"))
            .organization(getFirstValue(node, "dcx-dai:organization/dcx-dai:name"))
            .build();
    }

    // TODO add display name
    public String getDisplayName() {
        return this.getInitials() + " " + this.getSurname();
    }
}
