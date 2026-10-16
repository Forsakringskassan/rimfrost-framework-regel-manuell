package se.fk.rimfrost.framework.regel.manuell.helpers;

import com.github.tomakehurst.wiremock.WireMockServer;
import se.fk.rimfrost.framework.regel.WireMockHandlaggning;
import java.util.HashMap;
import java.util.Map;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockRegelManuell extends WireMockHandlaggning
{

   public static final String DEFAULT_UPPGIFT_ID = "11e53b18-e9ac-4707-825b-a1cb80689c29";

   /**
    * Handläggning id used by the abstract test classes. A default handläggning is stubbed for it, so
    * those tests don't depend on mapping files.
    */
   public static final String DEFAULT_HANDLAGGNING_ID = "5367f6b8-cc4a-11f0-8de9-199901011234";

   /**
    * Priority of the default handläggning, identity, SID and permissions stubs. WireMock uses the
    * mapping with the lowest priority value, so this is (close to) the lowest possible precedence:
    * any mapping or stub defined by a rule implementation, with or without an explicit priority,
    * takes precedence over these defaults.
    */
   private static final int DEFAULT_STUB_PRIORITY = Integer.MAX_VALUE - 1;

   private static final String DEFAULT_HANDLAGGNING_JSON = """
         {
           "handlaggning": {
             "id": "%s",
             "yrkande": {
               "id": "81234d56-7890-4abc-1234-56789abcdef0",
               "version": 1,
               "erbjudandeId": "f35c574d-e2a3-42ac-9ccb-835a24e692fe",
               "yrkandedatum": "2026-01-22T13:30:11.155427+01:00",
               "yrkandestatus": "YRKAT",
               "yrkandeFrom": "2025-01-10T16:15:50Z",
               "yrkandeTom": "2025-01-10T16:15:50Z",
               "avsikt": "NY",
               "individYrkandeRoller": [
                 {
                   "individ": { "typId": "PERSONNR", "varde": "19901010-1234" },
                   "yrkandeRollId": "d1b6721d-a3d8-4fd1-aae8-6ba11849e9bc"
                 }
               ],
               "produceradeResultat": []
             },
             "processinstansId": "e7f3c5a2-1234-4abc-9876-56789abcdef0",
             "version": 1,
             "skapadTS": "2026-01-22T13:31:00.8742683+01:00",
             "avslutadTS": null,
             "handlaggningspecifikationId": "71234567-89ab-4cde-9012-3456789abcde"
           }
         }
         """.formatted(DEFAULT_HANDLAGGNING_ID);

   @Override
   public Map<String, String> start()
   {
      var map = super.start();
      stubDefaultOul();
      stubDefaultIdentity();
      stubDefaultSidAndPermissions();
      stubDefaultHandlaggning();
      return map;
   }

   /**
    * OUL service: creating and ending an uppgift succeeds with {@link #DEFAULT_UPPGIFT_ID}.
    */
   private void stubDefaultOul()
   {
      server.stubFor(post(urlPathEqualTo("/uppgifter"))
            .willReturn(okJson("{\"uppgiftId\":\"" + DEFAULT_UPPGIFT_ID + "\",\"status\":\"NY\"}")));
      server.stubFor(post(urlPathMatching("/uppgifter/.+/end"))
            .willReturn(okJson("{\"uppgiftId\":\"" + DEFAULT_UPPGIFT_ID + "\",\"status\":\"AVSLUTAD\"}")));
   }

   /**
    * Identity service: "Bearer &lt;typId&gt;:&lt;varde&gt;" resolves to that identity, anything else is 401.
    */
   private void stubDefaultIdentity()
   {
      server.stubFor(get(urlPathEqualTo("/identity"))
            .atPriority(DEFAULT_STUB_PRIORITY)
            .withHeader("Authorization", matching("Bearer .+:.+"))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody("{{regexExtract request.headers.Authorization 'Bearer (.+):(.+)' 'parts'}}"
                        + "{\"identity\": {\"typId\": \"{{parts.0}}\", \"varde\": \"{{parts.1}}\"}}")
                  .withTransformers("response-template")));
      server.stubFor(get(urlPathEqualTo("/identity"))
            .atPriority(DEFAULT_STUB_PRIORITY + 1)
            .willReturn(unauthorized()));
   }

   /**
    * SID and permissions services: no SID, no SID permission.
    */
   private void stubDefaultSidAndPermissions()
   {
      server.stubFor(post(urlPathEqualTo("/sid/status"))
            .atPriority(DEFAULT_STUB_PRIORITY)
            .willReturn(okJson("{\"sid\": false}")));
      server.stubFor(post(urlPathEqualTo("/hasSidPermission"))
            .atPriority(DEFAULT_STUB_PRIORITY)
            .willReturn(okJson("false")));
   }

   /**
    * Handläggning service: the handläggning used by the abstract test classes
    * ({@link #DEFAULT_HANDLAGGNING_ID}) can be read and updated.
    */
   private void stubDefaultHandlaggning()
   {
      server.stubFor(get(urlPathEqualTo(handlaggningEndpoint + DEFAULT_HANDLAGGNING_ID))
            .atPriority(DEFAULT_STUB_PRIORITY)
            .willReturn(okJson(DEFAULT_HANDLAGGNING_JSON)));
      server.stubFor(put(urlPathEqualTo(handlaggningEndpoint + DEFAULT_HANDLAGGNING_ID))
            .atPriority(DEFAULT_STUB_PRIORITY)
            .willReturn(okJson(DEFAULT_HANDLAGGNING_JSON)));
   }

   /**
    * Defines wiremock mappings for manual rules.
    * <p>
    * Note: Introducing this class creates a placeholder for adding mappings to all manual rules
    * (even if initially no such mappings have been identified).</p>
    *
    * @param server active WireMock server
    * @return property mappings
    */
   @SuppressWarnings("UnnecessaryLocalVariable")
   @Override
   protected Map<String, String> wiremockMapping(WireMockServer server)
   {
      Map<String, String> map = new HashMap<>(super.wiremockMapping(server));
      // Add more mappings if manual rules need to define extra mappings
      // map.put("something", "value");
      map.put("oul.api.base-url", server.baseUrl());
      map.put("sid.api.base-url", server.baseUrl());
      map.put("permissions.api.base-url", server.baseUrl());
      map.put("quarkus.rest-client.identity-api.url", server.baseUrl());
      return map;
   }
}
